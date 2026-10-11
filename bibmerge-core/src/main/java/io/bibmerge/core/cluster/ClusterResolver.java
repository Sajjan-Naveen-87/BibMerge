package io.bibmerge.core.cluster;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;

/**
 * Turns MERGE decisions into clusters with {@link UnionFind}.
 *
 * <p>Union-find makes the result transitive: if A merges with B and B with C, all
 * three land in one cluster even though A and C were never compared directly.
 * Validation guards (Stage 7.5: maxSize 8, maxYearSpan 2) are a follow-up.
 */
public final class ClusterResolver {

    /**
     * @param records every record, in input order; each appears in exactly one cluster
     * @param merges  MERGE decisions only; their records are elements of {@code records}
     * @return clusters ordered by their first member's position in {@code records}
     */
    public List<Cluster> resolve(List<NormalizedRecord> records, List<PairDecision> merges) {
        // By identity, not equals: each element of the list is its own record.
        Map<NormalizedRecord, Integer> positions = new IdentityHashMap<>();
        for (int i = 0; i < records.size(); i++) {
            positions.put(records.get(i), i);
        }

        UnionFind sets = new UnionFind(records.size());
        for (PairDecision merge : merges) {
            if (merge.decision() != Decision.MERGE) {
                throw new IllegalArgumentException("only MERGE decisions form clusters, got "
                        + merge.decision() + " for " + merge.pair().left().source().location());
            }
            sets.union(position(positions, merge.pair().left()),
                    position(positions, merge.pair().right()));
        }

        return sets.components().stream()
                .map(component -> new Cluster(component.stream().map(records::get).toList()))
                .toList();
    }

    private static int position(Map<NormalizedRecord, Integer> positions, NormalizedRecord record) {
        Integer position = positions.get(record);
        if (position == null) {
            throw new IllegalArgumentException(
                    "merge refers to a record not in the input: " + record.source().location());
        }
        return position;
    }
}
