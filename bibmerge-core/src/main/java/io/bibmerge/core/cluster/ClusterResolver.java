package io.bibmerge.core.cluster;

import java.util.List;

import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;

/**
 * Turns MERGE decisions into clusters with {@link UnionFind}.
 *
 * <p>TODO(vamshi): implement {@link #resolve}. Validation guards (Stage 7.5:
 * maxSize 8, maxYearSpan 2) are a follow-up; get plain union-find working first.
 */
public final class ClusterResolver {

    /**
     * @param records every record, in input order; each appears in exactly one cluster
     * @param merges  MERGE decisions only; their records are elements of {@code records}
     * @return clusters ordered by their first member's position in {@code records}
     */
    public List<Cluster> resolve(List<NormalizedRecord> records, List<PairDecision> merges) {
        throw new UnsupportedOperationException("TODO(vamshi): ClusterResolver.resolve");
    }
}
