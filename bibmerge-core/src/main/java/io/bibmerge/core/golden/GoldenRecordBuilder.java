package io.bibmerge.core.golden;

import java.util.Set;

import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.Cluster;

/**
 * Builds the {@link CanonicalEntry} for one cluster (design doc Stage 8).
 *
 * <p>TODO(vamshi): implement {@link #build}. Rules, in order, per field:
 * <ol>
 *   <li>a value from a record with a DOI wins;</li>
 *   <li>otherwise the longest non-blank value;</li>
 *   <li>ties go to the earliest member.</li>
 * </ol>
 * Fields present in only one member are carried over. Entry type: the version of
 * record's type when the cluster mixes a preprint and a published version, else the
 * most common type. Key: from {@link KeyMinter}. Retired keys: every member's key.
 */
public final class GoldenRecordBuilder {

    private final KeyMinter keyMinter;

    public GoldenRecordBuilder(KeyMinter keyMinter) {
        this.keyMinter = keyMinter;
    }

    /** @param taken keys already issued in this run; the minted key is added to it */
    public CanonicalEntry build(Cluster cluster, Set<String> taken) {
        throw new UnsupportedOperationException("TODO(vamshi): GoldenRecordBuilder.build");
    }
}
