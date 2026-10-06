package io.bibmerge.core.serialize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.bibmerge.core.model.CanonicalEntry;

/**
 * Writes canonical entries as a {@code .bib} file.
 *
 * <ul>
 *   <li>preambles first, each as {@code @preamble{"..."}};</li>
 *   <li>entries sorted by key; fields in a fixed order (author, title, then
 *       alphabetical), each {@code  name = {value},} on its own line;</li>
 *   <li>values written verbatim inside braces: they are raw BibTeX already, so brace
 *       protection such as {@code {RSA}} survives unchanged;</li>
 *   <li>under {@link Dialect#BIBLATEX}, a non-empty retired-key list becomes
 *       {@code ids = {k1,k2}}, so citations of the old keys still resolve (FR10);</li>
 *   <li>round trip: parsing the output with BibParser gives back the same fields.</li>
 * </ul>
 * Output uses {@code \n} line endings on every platform, so the same input gives
 * byte-identical files (NFR2).
 */
public final class BibWriter {

    private static final List<String> LEADING_FIELDS = List.of("author", "title");

    private static final Comparator<String> FIELD_ORDER = Comparator
            .comparingInt(BibWriter::leadingRank)
            .thenComparing(Comparator.naturalOrder());

    public String write(List<CanonicalEntry> entries, List<String> preambles, Dialect dialect) {
        List<String> blocks = new ArrayList<>();
        for (String preamble : preambles) {
            blocks.add("@preamble{\"" + preamble + "\"}\n");
        }
        entries.stream()
                .sorted(Comparator.comparing(CanonicalEntry::key))
                .forEach(entry -> blocks.add(entry(entry, dialect)));
        return String.join("\n", blocks);
    }

    private static String entry(CanonicalEntry entry, Dialect dialect) {
        Map<String, String> fields = new TreeMap<>(FIELD_ORDER);
        fields.putAll(entry.fields());
        if (dialect == Dialect.BIBLATEX && !entry.retiredKeys().isEmpty()) {
            fields.put("ids", ids(entry.retiredKeys(), fields.get("ids")));
        }

        StringBuilder out = new StringBuilder();
        out.append('@').append(entry.entryType()).append('{').append(entry.key()).append(",\n");
        fields.forEach((name, value) ->
                out.append("  ").append(name).append(" = {").append(value).append("},\n"));
        return out.append("}\n").toString();
    }

    /** Retired keys first, then any aliases a source already declared in its own ids. */
    private static String ids(List<String> retiredKeys, String existing) {
        Set<String> keys = new LinkedHashSet<>(retiredKeys);
        if (existing != null) {
            for (String alias : existing.split(",")) {
                if (!alias.isBlank()) {
                    keys.add(alias.strip());
                }
            }
        }
        return String.join(",", keys);
    }

    private static int leadingRank(String name) {
        int rank = LEADING_FIELDS.indexOf(name);
        return rank < 0 ? LEADING_FIELDS.size() : rank;
    }
}
