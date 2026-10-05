package io.bibmerge.core.match;

import java.util.List;

import io.bibmerge.core.block.AllPairsBlocker;
import io.bibmerge.core.block.Blocker;
import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.FeatureVector;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;

/**
 * Stages 4–6 end to end: block, extract features, score, decide.
 *
 * <p>This is the hand-off to clustering (Stage 7): MERGE decisions become union-find
 * edges, REVIEW decisions go to the review queue.
 */
public final class Matcher {

    /**
     * @param decisions   every MERGE and REVIEW decision; DISTINCT pairs are dropped
     * @param comparisons number of candidate pairs compared
     */
    public record Result(List<PairDecision> decisions, int comparisons) {

        public List<PairDecision> merges() {
            return decisions.stream().filter(d -> d.decision() == Decision.MERGE).toList();
        }

        public List<PairDecision> reviews() {
            return decisions.stream().filter(d -> d.decision() == Decision.REVIEW).toList();
        }
    }

    private final Blocker blocker;
    private final FeatureExtractor features = new FeatureExtractor();
    private final Scorer scorer;
    private final DecisionPolicy policy;

    public Matcher(Blocker blocker, Scorer scorer, DecisionConfig config) {
        this.blocker = blocker;
        this.scorer = scorer;
        this.policy = new DecisionPolicy(config);
    }

    public static Matcher withDefaults() {
        return new Matcher(new AllPairsBlocker(), new RuleScorer(), DecisionConfig.defaults());
    }

    public Result match(List<NormalizedRecord> records) {
        List<CandidatePair> candidates = blocker.candidates(records);
        List<PairDecision> decisions = candidates.stream()
                .map(this::decide)
                .filter(d -> d.decision() != Decision.DISTINCT)
                .toList();
        return new Result(decisions, candidates.size());
    }

    public PairDecision decide(CandidatePair pair) {
        FeatureVector vector = features.extract(pair.left(), pair.right());
        return policy.decide(pair, vector, scorer.score(vector));
    }
}
