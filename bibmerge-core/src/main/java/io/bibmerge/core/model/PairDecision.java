package io.bibmerge.core.model;

/**
 * The outcome for one candidate pair.
 *
 * @param pair     the pair
 * @param decision MERGE, REVIEW or DISTINCT
 * @param score    rule-scorer score in [0, 1]
 * @param rule     the precedence-ladder rule that decided, e.g. {@code "identifier-agree"}
 * @param features the evidence, shown to the reviewer (NFR5)
 */
public record PairDecision(CandidatePair pair, Decision decision, double score, String rule,
        FeatureVector features) {
}
