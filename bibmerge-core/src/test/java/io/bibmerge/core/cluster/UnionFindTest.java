package io.bibmerge.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class UnionFindTest {

    @Test
    void everyElementStartsAlone() {
        UnionFind sets = new UnionFind(3);

        assertEquals(List.of(List.of(0), List.of(1), List.of(2)), sets.components());
    }

    @Test
    void unionJoinsAndReportsChange() {
        UnionFind sets = new UnionFind(4);

        assertTrue(sets.union(0, 2));
        assertFalse(sets.union(2, 0));
        assertEquals(sets.find(0), sets.find(2));
        assertNotEquals(sets.find(0), sets.find(1));
    }

    @Test
    void unionIsTransitive() {
        UnionFind sets = new UnionFind(5);
        sets.union(3, 1);
        sets.union(1, 4);

        assertEquals(List.of(List.of(0), List.of(1, 3, 4), List.of(2)), sets.components());
    }

    @Test
    void handlesLongChains() {
        int n = 100_000;
        UnionFind sets = new UnionFind(n);
        for (int i = 1; i < n; i++) {
            sets.union(i - 1, i);
        }

        assertEquals(1, sets.components().size());
    }

    @Test
    void emptyHasNoComponents() {
        assertTrue(new UnionFind(0).components().isEmpty());
    }
}
