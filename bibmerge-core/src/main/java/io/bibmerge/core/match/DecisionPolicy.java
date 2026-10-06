package io.bibmerge.core.match;

import java.util.regex.Pattern;

import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.Feature;
import io.bibmerge.core.model.FeatureVector;
import io.bibmerge.core.model.IdAgreement;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.model.VersionRole;

/**
 * Stage 6: the precedence ladder. The first rule that fires decides; caps can only
 * lower a MERGE to REVIEW. The rule name is recorded on every decision.
 *
 * <p>Rules 1–2 (human MUST_LINK / CANNOT_LINK constraints) arrive with the
 * ConstraintStore. One rule is added to the design's ladder: {@code title-dissimilar}.
 * Without it, two different papers by the same authors in the same venue and year
 * score about 0.68 on author, year and venue agreement alone and flood the review
 * queue. To be revisited when thresholds are fitted.
 */
public final class DecisionPolicy {

    /** Titles this close are "not strongly dissimilar" for identifier agreement. */
    static final double TITLE_AGREES = 0.80;

    static final double TITLE_DISSIMILAR_JACCARD = 0.30;

    private static final Pattern PART = Pattern.compile(
            "\\bpart (i{1,3}|iv|v|vi{0,3}|ix|x|\\d+)\\b");

    private final DecisionConfig config;

    public DecisionPolicy(DecisionConfig config) {
        this.config = config;
    }

    public PairDecision decide(CandidatePair pair, FeatureVector features, double score) {
        NormalizedRecord a = pair.left();
        NormalizedRecord b = pair.right();
        IdAgreement ids = features.idAgree();
        boolean versionPair = isPreprintAndVersionOfRecord(a, b);
        double titleJw = features.get(Feature.TITLE_JW).orElse(-1);
        double titleJaccard = features.get(Feature.TITLE_JACCARD).orElse(-1);

        // Rule 3: vetoes. Two different arXiv or ePrint ids are never one work (revisions
        // keep their id); a DOI conflict is excused only for a preprint/published pair.
        if (preprintIdsConflict(a, b)) {
            return decision(pair, Decision.DISTINCT, score, "veto:preprint-id-conflict",
                    features);
        }
        if (ids == IdAgreement.CONFLICT && !versionPair) {
            return decision(pair, Decision.DISTINCT, score, "veto:identifier-conflict", features);
        }
        if (features.get(Feature.TYPE_COMPAT).orElse(1.0) == 0.0) {
            return decision(pair, Decision.DISTINCT, score, "veto:entry-type", features);
        }
        if (!partNumber(a.title()).equals(partNumber(b.title()))) {
            return decision(pair, Decision.DISTINCT, score, "veto:part-number", features);
        }

        // Rule 4: a large year gap without identifier agreement caps at REVIEW.
        String cap = null;
        if (features.get(Feature.YEAR_DELTA).isPresent() && Math.abs(a.year() - b.year()) >= 3
                && ids != IdAgreement.AGREE) {
            cap = "cap:year-gap";
        }

        // Rule 5: version policy, consulted before any merge rule (ADR-9).
        if (versionPair && config.versionPolicy() == VersionPolicy.KEEP_DISTINCT) {
            return decision(pair, Decision.DISTINCT, score, "version-policy:keep-distinct",
                    features);
        }
        if (isConferenceVsJournal(a, b)) {
            cap = "cap:conference-vs-journal";
        }

        // Rules 6–7: identifier agreement.
        if (ids == IdAgreement.AGREE) {
            if (titleJw < 0 || titleJw >= TITLE_AGREES) {
                return capped(pair, Decision.MERGE, score, "identifier-agree", cap, features);
            }
            return decision(pair, Decision.REVIEW, score, "identifier-agree-title-mismatch",
                    features);
        }

        // Rule 8: deterministic key.
        if (a.deterministicKey().isPresent()
                && a.deterministicKey().equals(b.deterministicKey())) {
            return capped(pair, Decision.MERGE, score, "deterministic-key", cap, features);
        }

        // Added rule: clearly different titles and no identifier evidence.
        if (titleJaccard >= 0 && titleJaccard < TITLE_DISSIMILAR_JACCARD
                && titleJw < TITLE_AGREES) {
            return decision(pair, Decision.DISTINCT, score, "title-dissimilar", features);
        }

        // Rule 9: thin evidence never auto-merges.
        if (features.evidenceMass() < config.minEvidence()) {
            Decision outcome = score >= config.tauLow() ? Decision.REVIEW : Decision.DISTINCT;
            return decision(pair, outcome, score, "thin-evidence", features);
        }

        // Rule 10: thresholds.
        if (score >= config.tauHigh()) {
            return capped(pair, Decision.MERGE, score, "score>=tauHigh", cap, features);
        }
        if (score >= config.tauLow()) {
            return decision(pair, Decision.REVIEW, score, "tauLow<=score<tauHigh", features);
        }
        return decision(pair, Decision.DISTINCT, score, "score<tauLow", features);
    }

    /** "part ii" from "lattices part ii", or empty. Part I and Part II are different works. */
    private static String partNumber(String title) {
        var m = PART.matcher(title);
        return m.find() ? m.group(1) : "";
    }

    private static boolean preprintIdsConflict(NormalizedRecord a, NormalizedRecord b) {
        return differ(a.ids().arxiv(), b.ids().arxiv()) || differ(a.ids().iacr(), b.ids().iacr());
    }

    private static boolean differ(String x, String y) {
        return x != null && y != null && !x.equals(y);
    }

    private static boolean isPreprintAndVersionOfRecord(NormalizedRecord a, NormalizedRecord b) {
        return (a.versionRole() == VersionRole.PREPRINT
                && b.versionRole() == VersionRole.VERSION_OF_RECORD)
                || (b.versionRole() == VersionRole.PREPRINT
                        && a.versionRole() == VersionRole.VERSION_OF_RECORD);
    }

    /** Conference paper vs its extended journal version: never automatic either way. */
    private static boolean isConferenceVsJournal(NormalizedRecord a, NormalizedRecord b) {
        boolean types = (a.entryType().equals("article") && b.entryType().equals("inproceedings"))
                || (a.entryType().equals("inproceedings") && b.entryType().equals("article"));
        return types && a.versionRole() == VersionRole.VERSION_OF_RECORD
                && b.versionRole() == VersionRole.VERSION_OF_RECORD;
    }

    private static PairDecision capped(CandidatePair pair, Decision outcome, double score,
            String rule, String cap, FeatureVector features) {
        if (outcome == Decision.MERGE && cap != null) {
            return decision(pair, Decision.REVIEW, score, rule + "+" + cap, features);
        }
        return decision(pair, outcome, score, rule, features);
    }

    private static PairDecision decision(CandidatePair pair, Decision outcome, double score,
            String rule, FeatureVector features) {
        return new PairDecision(pair, outcome, score, rule, features);
    }
}
