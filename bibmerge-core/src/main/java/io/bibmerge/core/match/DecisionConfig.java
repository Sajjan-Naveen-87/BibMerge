package io.bibmerge.core.match;

/**
 * Decision thresholds (design doc §8). Starting values, to be fitted on the
 * development split, not constants we believe in.
 *
 * @param tauHigh       at or above: MERGE
 * @param tauLow        below: DISTINCT; between the two: REVIEW
 * @param minEvidence   below this evidence mass, never auto-merge
 * @param versionPolicy preprint vs version-of-record handling
 */
public record DecisionConfig(double tauHigh, double tauLow, double minEvidence,
        VersionPolicy versionPolicy) {

    public static DecisionConfig defaults() {
        return new DecisionConfig(0.86, 0.62, 0.55, VersionPolicy.MERGE_WITH_VERSION_TAGS);
    }
}
