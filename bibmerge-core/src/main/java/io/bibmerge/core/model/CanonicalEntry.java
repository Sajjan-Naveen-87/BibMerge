package io.bibmerge.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The golden record for one cluster (design doc Stage 8).
 *
 * @param key         minted citation key, frozen once issued (ADR-8)
 * @param entryType   lower-cased entry type
 * @param fields      chosen raw field values, lower-cased names
 * @param sources     every source record merged into this entry
 * @param retiredKeys source keys other than {@code key}, still resolvable (FR10)
 */
public record CanonicalEntry(String key, String entryType, Map<String, String> fields,
        List<SourceRecord> sources, List<String> retiredKeys) {

    public CanonicalEntry {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        sources = List.copyOf(sources);
        retiredKeys = List.copyOf(retiredKeys);
    }
}
