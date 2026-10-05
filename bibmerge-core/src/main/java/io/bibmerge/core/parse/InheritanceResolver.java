package io.bibmerge.core.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.ParseDiagnostic.Severity;
import io.bibmerge.core.model.SourceRecord;

/**
 * Stage 1.5: resolves classic BibTeX {@code crossref} within one file.
 *
 * <p>A child inherits every field it does not define. When the parent is a container
 * ({@code @proceedings}, {@code @book}, ...), the parent's {@code title} becomes the
 * child's {@code booktitle} rather than its {@code title}. Parents that are inherited
 * from are flagged as containers so they are not matched against their own children.
 * The raw fields stay untouched on the record; only the effective view changes.
 *
 * <p>Not yet handled: biblatex {@code xdata}, and crossref across files.
 */
final class InheritanceResolver {

    private static final Set<String> CONTAINER_TYPES = Set.of(
            "proceedings", "book", "collection", "mvproceedings", "mvbook", "mvcollection");

    /** Fields describing the parent itself, never inherited. */
    private static final Set<String> NOT_INHERITED = Set.of(
            "crossref", "xdata", "ids", "key", "title", "subtitle", "titleaddon",
            "shorttitle", "sorttitle", "indextitle", "label");

    private InheritanceResolver() {
    }

    static List<SourceRecord> resolve(List<SourceRecord> records,
            List<ParseDiagnostic> diagnostics) {
        Map<String, SourceRecord> byKey = new HashMap<>();
        for (SourceRecord record : records) {
            byKey.putIfAbsent(normalizeKey(record.key()), record);
        }

        Set<String> parents = new HashSet<>();
        for (SourceRecord record : records) {
            record.field("crossref").map(InheritanceResolver::normalizeKey)
                    .filter(byKey::containsKey)
                    .ifPresent(parents::add);
        }

        return records.stream().map(record -> {
            boolean container = parents.contains(normalizeKey(record.key()));
            String crossref = record.fields().get("crossref");
            if (crossref == null || crossref.isBlank()) {
                return container ? record.withInheritance(record.fields(), true) : record;
            }
            SourceRecord parent = byKey.get(normalizeKey(crossref));
            if (parent == null) {
                diagnostics.add(new ParseDiagnostic(record.sourceFile(), record.line(),
                        Severity.WARNING, "crossref '" + crossref.strip() + "' of '"
                                + record.key() + "' is not defined in this file"));
                return record;
            }
            if (parent == record) {
                return record;
            }
            return record.withInheritance(inherit(record, parent), container);
        }).toList();
    }

    private static Map<String, String> inherit(SourceRecord child, SourceRecord parent) {
        Map<String, String> effective = new LinkedHashMap<>(child.fields());
        boolean parentIsContainer = CONTAINER_TYPES.contains(parent.entryType());
        parent.fields().forEach((name, value) -> {
            if (!NOT_INHERITED.contains(name)) {
                effective.putIfAbsent(name, value);
            }
        });
        String parentTitle = parent.fields().get("title");
        if (parentIsContainer && parentTitle != null) {
            effective.putIfAbsent("booktitle", parentTitle);
        }
        return effective;
    }

    private static String normalizeKey(String key) {
        return key.strip().toLowerCase(Locale.ROOT);
    }
}
