package io.bibmerge.core.pipeline;

import java.util.List;

import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.model.ParseDiagnostic;

/**
 * Output of one merge run.
 *
 * @param bib         the merged {@code .bib} text
 * @param entries     canonical entries, in output order
 * @param merges      the MERGE decisions that formed the clusters
 * @param reviews     pairs left for a human; not merged
 * @param diagnostics problems found while parsing
 * @param records     number of input records
 * @param comparisons number of candidate pairs compared
 */
public record MergeResult(String bib, List<CanonicalEntry> entries, List<PairDecision> merges,
        List<PairDecision> reviews, List<ParseDiagnostic> diagnostics, int records,
        int comparisons) {
}
