package io.bibmerge.core.cluster;

import java.util.List;

/**
 * Disjoint sets over the integers {@code 0..size-1}, with path compression and
 * union by size.
 *
 * <p>TODO(vamshi): implement. Spec: doc/tasks/vamshi-stages-7-9.md. Tests:
 * UnionFindTest (remove its {@code @Disabled}).
 */
public final class UnionFind {

    public UnionFind(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }

    /** Representative of the set containing {@code x}. */
    public int find(int x) {
        throw new UnsupportedOperationException("TODO(vamshi): UnionFind.find");
    }

    /** Joins the sets of {@code a} and {@code b}; false if already joined. */
    public boolean union(int a, int b) {
        throw new UnsupportedOperationException("TODO(vamshi): UnionFind.union");
    }

    /**
     * Every set, each listed in ascending order, sets ordered by their smallest
     * element. Deterministic output matters: it fixes the order of the merged file.
     */
    public List<List<Integer>> components() {
        throw new UnsupportedOperationException("TODO(vamshi): UnionFind.components");
    }
}
