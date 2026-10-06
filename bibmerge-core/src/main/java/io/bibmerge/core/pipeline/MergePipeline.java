package io.bibmerge.core.pipeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.bibmerge.core.cluster.ClusterResolver;
import io.bibmerge.core.golden.GoldenRecordBuilder;
import io.bibmerge.core.golden.KeyMinter;
import io.bibmerge.core.match.Matcher;
import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.normalize.RecordNormalizer;
import io.bibmerge.core.serialize.BibWriter;
import io.bibmerge.core.serialize.Dialect;

/**
 * Stages 2–9 over already-parsed files: normalize, match, cluster, build golden
 * records, serialize. Stateless: every run starts from the given files.
 */
public final class MergePipeline {

    private final Matcher matcher;
    private final ClusterResolver clusters;
    private final GoldenRecordBuilder golden;
    private final BibWriter writer;
    private final Dialect dialect;

    public MergePipeline(Matcher matcher, ClusterResolver clusters, GoldenRecordBuilder golden,
            BibWriter writer, Dialect dialect) {
        this.matcher = matcher;
        this.clusters = clusters;
        this.golden = golden;
        this.writer = writer;
        this.dialect = dialect;
    }

    public static MergePipeline withDefaults(Dialect dialect) {
        return new MergePipeline(Matcher.withDefaults(), new ClusterResolver(),
                new GoldenRecordBuilder(new KeyMinter()), new BibWriter(), dialect);
    }

    public MergeResult run(List<ParseResult> inputs) {
        List<NormalizedRecord> records = RecordNormalizer.normalizeAll(
                inputs.stream().flatMap(input -> input.records().stream()).toList());
        Matcher.Result matched = matcher.match(records);

        Set<String> taken = new HashSet<>();
        List<CanonicalEntry> entries = new ArrayList<>();
        for (Cluster cluster : clusters.resolve(records, matched.merges())) {
            entries.add(golden.build(cluster, taken));
        }

        List<String> preambles = inputs.stream()
                .flatMap(input -> input.preambles().stream()).distinct().toList();
        List<ParseDiagnostic> diagnostics = inputs.stream()
                .flatMap(input -> input.diagnostics().stream()).toList();

        return new MergeResult(writer.write(entries, preambles, dialect), entries,
                matched.merges(), matched.reviews(), diagnostics, records.size(),
                matched.comparisons());
    }
}
