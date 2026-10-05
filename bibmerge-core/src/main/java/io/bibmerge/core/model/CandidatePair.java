package io.bibmerge.core.model;

/**
 * Two records proposed for comparison by blocking (design doc Stage 4).
 *
 * @param left    first record
 * @param right   second record
 * @param blocker name of the blocker that proposed the pair
 */
public record CandidatePair(NormalizedRecord left, NormalizedRecord right, String blocker) {
}
