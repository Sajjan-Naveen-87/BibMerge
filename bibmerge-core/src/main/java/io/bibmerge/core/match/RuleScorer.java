package io.bibmerge.core.match;

import java.util.Map;

import io.bibmerge.core.model.Feature;
import io.bibmerge.core.model.FeatureVector;

/**
 * {@code s = Σ wᵢ·fᵢ / Σ wᵢ} over present features only, with the weights of
 * {@link Feature} (design doc Stage 6). Zero when no feature is present.
 */
public final class RuleScorer implements Scorer {

    @Override
    public double score(FeatureVector features) {
        double weighted = 0.0;
        double totalWeight = 0.0;
        for (Map.Entry<Feature, Double> entry : features.values().entrySet()) {
            weighted += entry.getKey().weight() * entry.getValue();
            totalWeight += entry.getKey().weight();
        }
        return totalWeight == 0.0 ? 0.0 : weighted / totalWeight;
    }
}
