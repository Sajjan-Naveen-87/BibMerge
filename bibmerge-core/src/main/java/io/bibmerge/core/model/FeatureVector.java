package io.bibmerge.core.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Feature values for one pair. A feature missing from {@code values} is ABSENT:
 * neither record had the evidence, which is different from the evidence disagreeing.
 *
 * @param values  present features, each in [0, 1]
 * @param idAgree identifier agreement
 */
public record FeatureVector(Map<Feature, Double> values, IdAgreement idAgree) {

    public FeatureVector {
        EnumMap<Feature, Double> copy = new EnumMap<>(Feature.class);
        copy.putAll(values);
        values = Collections.unmodifiableMap(copy);
    }

    public OptionalDouble get(Feature feature) {
        Double value = values.get(feature);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /** Sum of the weights of present features. */
    public double evidenceMass() {
        return values.keySet().stream().mapToDouble(Feature::weight).sum();
    }
}
