package io.bibmerge.core.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.NormalizedRecord;

/**
 * Proposes every pair: blocking recall 1.0 by construction, O(n²) comparisons.
 *
 * <p>Fine for the few hundred records in a handful of papers. The MinHash-LSH,
 * author-year and sorted-neighbourhood blockers of the design replace it behind the
 * same {@link Blocker} interface when libraries grow.
 *
 * <p>A container ({@code @proceedings}) is never paired with a record that inherits
 * from it (design doc Stage 1.5).
 */
public final class AllPairsBlocker implements Blocker {

    public static final String NAME = "all-pairs";

    @Override
    public List<CandidatePair> candidates(List<NormalizedRecord> records) {
        List<CandidatePair> pairs = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            for (int j = i + 1; j < records.size(); j++) {
                NormalizedRecord left = records.get(i);
                NormalizedRecord right = records.get(j);
                if (!isContainerOf(left, right) && !isContainerOf(right, left)) {
                    pairs.add(new CandidatePair(left, right, NAME));
                }
            }
        }
        return pairs;
    }

    private static boolean isContainerOf(NormalizedRecord parent, NormalizedRecord child) {
        if (!parent.source().container()
                || !parent.source().sourceFile().equals(child.source().sourceFile())) {
            return false;
        }
        String crossref = child.source().fields().get("crossref");
        return crossref != null && crossref.strip().toLowerCase(Locale.ROOT)
                .equals(parent.source().key().strip().toLowerCase(Locale.ROOT));
    }
}
