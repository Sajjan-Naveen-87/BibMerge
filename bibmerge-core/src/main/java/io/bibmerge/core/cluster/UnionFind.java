package io.bibmerge.core.cluster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Disjoint sets over the integers {@code 0..size-1}, with path compression and
 * union by size.
 *
 * <p>Together the two keep every tree almost flat, so each operation is effectively
 * constant time (inverse Ackermann) and clustering costs one pass over the MERGE edges.
 */
public final class UnionFind {

    private final int[] parent;
    private final int[] size;

    public UnionFind(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        this.parent = new int[size];
        this.size = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
            this.size[i] = 1;
        }
    }

    /**
     * Representative of the set containing {@code x}. Iterative rather than recursive,
     * so a long chain cannot overflow the stack.
     */
    public int find(int x) {
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
        }
        // Path compression: point everything on the walk straight at the root.
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /** Joins the sets of {@code a} and {@code b}; false if already joined. */
    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        // Union by size: hang the smaller tree under the larger one.
        if (size[rootA] < size[rootB]) {
            int swap = rootA;
            rootA = rootB;
            rootB = swap;
        }
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        return true;
    }

    /**
     * Every set, each listed in ascending order, sets ordered by their smallest
     * element. Deterministic output matters: it fixes the order of the merged file.
     */
    public List<List<Integer>> components() {
        // Visiting elements in ascending order yields both orderings directly.
        Map<Integer, List<Integer>> byRoot = new LinkedHashMap<>();
        for (int i = 0; i < parent.length; i++) {
            byRoot.computeIfAbsent(find(i), root -> new ArrayList<>()).add(i);
        }
        return byRoot.values().stream().map(List::copyOf).toList();
    }
}
