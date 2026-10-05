package io.bibmerge.core.match;

import io.bibmerge.core.model.FeatureVector;

/** Turns features into a score in [0, 1] (ADR-6: rule scorer now, learned later). */
public interface Scorer {

    double score(FeatureVector features);
}
