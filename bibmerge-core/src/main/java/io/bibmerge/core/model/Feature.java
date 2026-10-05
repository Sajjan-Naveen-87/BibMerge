package io.bibmerge.core.model;

/** Similarity features and their rule-scorer weights (design doc Stage 5). */
public enum Feature {
    TITLE_JW(0.22),
    TITLE_JACCARD(0.18),
    TITLE_IDF_OVERLAP(0.15),
    AUTHOR_SET_F1(0.15),
    AUTHOR1_EXACT(0.08),
    YEAR_DELTA(0.07),
    VENUE_SIM(0.06),
    PAGE_VOL_AGREE(0.04),
    TYPE_COMPAT(0.03),
    TITLE_LEN_RATIO(0.02);

    private final double weight;

    Feature(double weight) {
        this.weight = weight;
    }

    public double weight() {
        return weight;
    }
}
