package io.bibmerge.core.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.bibmerge.core.match.Constraints;
import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;

/**
 * Turns MERGE decisions into clusters with {@link UnionFind}.
 *
 * <p>Union-find makes the result transitive: if A merges with B and B with C, all
 * three land in one cluster even though A and C were never compared directly.
 *
 * <p>When a human has said two records are different works but a chain of merges still
 * joins them, the chain is cut at its bottleneck edge (design doc Stage 7): insert the
 * MERGE edges in descending score order, human merges first, and remove the edge whose
 * insertion first joins the two records. Repeat until no such pair is joined. Each round
 * removes one edge and edges are never re-added, so it ends after at most |edges| rounds.
 * Validation guards (Stage 7.5: maxSize 8, maxYearSpan 2) are a follow-up.
 */
public final class ClusterResolver {

    /**
     * @param clusters clusters ordered by their first member's position in the input
     * @param cut      MERGE edges removed to keep human keep-separate pairs apart, in
     *                 input order; they belong in front of a reviewer again
     */
    public record Resolution(List<Cluster> clusters, List<PairDecision> cut) {

        public Resolution {
            clusters = List.copyOf(clusters);
            cut = List.copyOf(cut);
        }
    }

    /** Human merges first, then higher scores; the sort is stable, so ties keep input order. */
    private static final Comparator<PairDecision> INSERTION_ORDER = Comparator
            .comparing((PairDecision d) -> !d.rule().startsWith("human:"))
            .thenComparing(PairDecision::score, Comparator.reverseOrder());

    /**
     * @param records every record, in input order; each appears in exactly one cluster
     * @param merges  MERGE decisions only; their records are elements of {@code records}
     * @return clusters ordered by their first member's position in {@code records}
     */
    public List<Cluster> resolve(List<NormalizedRecord> records, List<PairDecision> merges) {
        return resolve(records, merges, Constraints.NONE).clusters();
    }

    public Resolution resolve(List<NormalizedRecord> records, List<PairDecision> merges,
            Constraints constraints) {
        // By identity, not equals: each element of the list is its own record.
        Map<NormalizedRecord, Integer> positions = new IdentityHashMap<>();
        for (int i = 0; i < records.size(); i++) {
            positions.put(records.get(i), i);
        }
        for (PairDecision merge : merges) {
            if (merge.decision() != Decision.MERGE) {
                throw new IllegalArgumentException("only MERGE decisions form clusters, got "
                        + merge.decision() + " for " + merge.pair().left().source().location());
            }
            position(positions, merge.pair().left());
            position(positions, merge.pair().right());
        }

        List<PairDecision> order = new ArrayList<>(merges);
        order.sort(INSERTION_ORDER);
        List<int[]> apart = keepSeparatePositions(records, constraints);
        Set<PairDecision> cut = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int round = 0; round <= order.size(); round++) {
            UnionFind sets = union(order, cut, positions, records.size(), null);
            int[] joined = firstJoined(sets, apart);
            if (joined == null) {
                List<Cluster> clusters = sets.components().stream()
                        .map(component -> new Cluster(component.stream().map(records::get).toList()))
                        .toList();
                return new Resolution(clusters, merges.stream().filter(cut::contains).toList());
            }
            List<PairDecision> bottleneck = new ArrayList<>(1);
            union(order, cut, positions, records.size(), new Stop(joined, bottleneck));
            cut.add(bottleneck.get(0));
        }
        throw new IllegalStateException("could not keep human keep-separate pairs apart");
    }

    /** When set, union() stops at the first edge that joins {@code pair} and records it. */
    private record Stop(int[] pair, List<PairDecision> edge) {
    }

    private static UnionFind union(List<PairDecision> order, Set<PairDecision> cut,
            Map<NormalizedRecord, Integer> positions, int size, Stop stop) {
        UnionFind sets = new UnionFind(size);
        for (PairDecision edge : order) {
            if (cut.contains(edge)) {
                continue;
            }
            sets.union(positions.get(edge.pair().left()), positions.get(edge.pair().right()));
            if (stop != null && sets.find(stop.pair()[0]) == sets.find(stop.pair()[1])) {
                stop.edge().add(edge);
                break;
            }
        }
        return sets;
    }

    private static int[] firstJoined(UnionFind sets, List<int[]> apart) {
        for (int[] pair : apart) {
            if (sets.find(pair[0]) == sets.find(pair[1])) {
                return pair;
            }
        }
        return null;
    }

    /** Every pair of distinct record positions a human said to keep apart, in input order. */
    private static List<int[]> keepSeparatePositions(List<NormalizedRecord> records,
            Constraints constraints) {
        Map<String, List<Integer>> byHash = new HashMap<>();
        for (int i = 0; i < records.size(); i++) {
            byHash.computeIfAbsent(records.get(i).source().contentHash(), h -> new ArrayList<>())
                    .add(i);
        }
        List<int[]> pairs = new ArrayList<>();
        for (String[] hashes : constraints.keepSeparatePairs()) {
            for (int p : byHash.getOrDefault(hashes[0], List.of())) {
                for (int q : byHash.getOrDefault(hashes[1], List.of())) {
                    if (p != q) {
                        pairs.add(new int[] {Math.min(p, q), Math.max(p, q)});
                    }
                }
            }
        }
        // Deterministic order (design doc: sort keep-separate pairs before repairing).
        pairs.sort(Comparator.<int[]>comparingInt(p -> p[0]).thenComparingInt(p -> p[1]));
        return pairs;
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
