package io.bibmerge.core.match;

import java.util.Set;

/**
 * Entry-type compatibility: 1.0 same type, 0.7 plausibly the same work, 0.0
 * incompatible (a veto: {@code book} vs {@code article}, a paper vs its proceedings).
 */
final class TypeCompatibility {

    /** Types that carry little type information; compatible with anything. */
    private static final Set<String> LOOSE = Set.of("misc", "unpublished", "online",
            "techreport", "manual");

    /** Pairs that can be two versions of one work. */
    private static final Set<Set<String>> COMPATIBLE = Set.of(
            Set.of("article", "inproceedings"),
            Set.of("inproceedings", "incollection"),
            Set.of("article", "incollection"),
            Set.of("inbook", "incollection"),
            Set.of("book", "inbook"),
            Set.of("thesis", "book"));

    private TypeCompatibility() {
    }

    static double score(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        if (LOOSE.contains(a) || LOOSE.contains(b) || COMPATIBLE.contains(Set.of(a, b))) {
            return 0.7;
        }
        return 0.0;
    }
}
