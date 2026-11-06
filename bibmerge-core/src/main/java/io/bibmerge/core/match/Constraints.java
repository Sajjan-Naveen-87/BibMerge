package io.bibmerge.core.match;

import java.util.HashSet;
import java.util.Set;

/**
 * Human decisions that outrank every score and veto (design doc Stage 6, rules 1–2).
 *
 * <p>A decision is about two records' content hashes, not their keys or files (ADR-5),
 * so it still applies when the same entry is uploaded again under another key or from
 * another file. Pairs are unordered. A later decision about a pair replaces an earlier
 * one; if a set somehow holds both, keep-separate wins, because a wrong merge loses a
 * citation and a missed one does not (NFR1).
 *
 * <p>Immutable: {@link #merge} and {@link #keepSeparate} return a new instance.
 */
public final class Constraints {

    public static final Constraints NONE = new Constraints(Set.of(), Set.of());

    private final Set<String> merge;
    private final Set<String> keepSeparate;

    private Constraints(Set<String> merge, Set<String> keepSeparate) {
        this.merge = Set.copyOf(merge);
        this.keepSeparate = Set.copyOf(keepSeparate);
    }

    /** A copy that also says the two records are the same work. */
    public Constraints merge(String hashA, String hashB) {
        String pair = pair(hashA, hashB);
        Set<String> m = new HashSet<>(merge);
        Set<String> k = new HashSet<>(keepSeparate);
        m.add(pair);
        k.remove(pair);
        return new Constraints(m, k);
    }

    /** A copy that also says the two records are different works. */
    public Constraints keepSeparate(String hashA, String hashB) {
        String pair = pair(hashA, hashB);
        Set<String> m = new HashSet<>(merge);
        Set<String> k = new HashSet<>(keepSeparate);
        k.add(pair);
        m.remove(pair);
        return new Constraints(m, k);
    }

    public boolean merges(String hashA, String hashB) {
        return merge.contains(pair(hashA, hashB));
    }

    public boolean keepsSeparate(String hashA, String hashB) {
        return keepSeparate.contains(pair(hashA, hashB));
    }

    /** Keep-separate pairs as {@code {hashA, hashB}}, for clustering. */
    public Set<String[]> keepSeparatePairs() {
        Set<String[]> pairs = new HashSet<>();
        for (String pair : keepSeparate) {
            pairs.add(pair.split("\\|", 2));
        }
        return pairs;
    }

    public boolean isEmpty() {
        return merge.isEmpty() && keepSeparate.isEmpty();
    }

    /** Order-independent; two identical records (same hash) are a valid pair too. */
    private static String pair(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}
