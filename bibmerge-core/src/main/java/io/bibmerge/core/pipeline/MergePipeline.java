package io.bibmerge.core.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /** Source keys of a cluster, lower-cased: BibTeX treats keys case-insensitively. */
    private static Set<String> sourceKeys(Cluster cluster) {
        Set<String> keys = new HashSet<>();
        for (NormalizedRecord member : cluster.members()) {
            keys.add(member.source().key().toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    /**
     * Drops retired keys that other entries also retire. When two papers use one key for
     * different works, an ids alias on both would make \cite{key} resolve to one of them
     * silently; with no alias the citation fails loudly and the user fixes it.
     */
    private static CanonicalEntry withoutAmbiguousAliases(CanonicalEntry entry,
            Map<String, Integer> sourceKeyUses) {
        List<String> kept = entry.retiredKeys().stream()
                .filter(key -> sourceKeyUses.getOrDefault(key.toLowerCase(Locale.ROOT), 0) <= 1)
                .toList();
        if (kept.size() == entry.retiredKeys().size()) {
            return entry;
        }
        return new CanonicalEntry(entry.key(), entry.entryType(), entry.fields(),
                entry.sources(), kept);
    }

    /** One warning per record whose key names a different work elsewhere. */
    private static List<ParseDiagnostic> ambiguousKeyWarnings(List<Cluster> clusters,
            Map<String, Integer> sourceKeyUses) {
        List<ParseDiagnostic> warnings = new ArrayList<>();
        for (Cluster cluster : clusters) {
            Set<String> warned = new HashSet<>();
            for (NormalizedRecord member : cluster.members()) {
                String key = member.source().key().toLowerCase(Locale.ROOT);
                if (sourceKeyUses.getOrDefault(key, 0) > 1 && warned.add(key)) {
                    warnings.add(new ParseDiagnostic(member.source().sourceFile(),
                            member.source().line(), ParseDiagnostic.Severity.WARNING,
                            "Citation key '" + key + "' names different works in different"
                                    + " files; it is not kept as an alias, so update"
                                    + " \\cite{" + member.source().key() + "} by hand"));
                }
            }
        }
        return warnings;
    }

    public MergeResult run(List<ParseResult> inputs) {
        List<NormalizedRecord> records = RecordNormalizer.normalizeAll(
                inputs.stream().flatMap(input -> input.records().stream()).toList());
        Matcher.Result matched = matcher.match(records);

        List<Cluster> resolved = clusters.resolve(records, matched.merges());
        Map<String, Integer> sourceKeyUses = new HashMap<>();
        for (Cluster cluster : resolved) {
            for (String key : sourceKeys(cluster)) {
                sourceKeyUses.merge(key, 1, Integer::sum);
            }
        }

        Set<String> issued = new HashSet<>();
        List<CanonicalEntry> entries = new ArrayList<>();
        for (Cluster cluster : resolved) {
            // A minted key must not be another entry's old key: that old key becomes an
            // ids alias there, and \cite{oldkey} would silently resolve to the wrong work.
            Set<String> own = sourceKeys(cluster);
            Set<String> blocked = new HashSet<>(issued);
            sourceKeyUses.forEach((key, uses) -> {
                if (!own.contains(key) || uses > 1) {
                    blocked.add(key);
                }
            });
            CanonicalEntry entry = golden.build(cluster, blocked);
            issued.add(entry.key());
            entries.add(withoutAmbiguousAliases(entry, sourceKeyUses));
        }

        List<String> preambles = inputs.stream()
                .flatMap(input -> input.preambles().stream()).distinct().toList();
        List<ParseDiagnostic> diagnostics = new ArrayList<>(inputs.stream()
                .flatMap(input -> input.diagnostics().stream()).toList());
        diagnostics.addAll(ambiguousKeyWarnings(resolved, sourceKeyUses));

        return new MergeResult(writer.write(entries, preambles, dialect), entries,
                matched.merges(), matched.reviews(), diagnostics, records.size(),
                matched.comparisons());
    }
}
