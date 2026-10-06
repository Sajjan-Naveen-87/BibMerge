package io.bibmerge.core.golden;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.SourceRecord;
import io.bibmerge.core.model.VersionRole;
import io.bibmerge.core.normalize.RecordNormalizer;

/**
 * Builds the {@link CanonicalEntry} for one cluster (design doc Stage 8).
 *
 * <p>Rules, in order, per field over every member's effective fields:
 * <ol>
 *   <li>when the cluster mixes a preprint and a published version, a value from the
 *       version of record wins, so an ePrint's year never lands on its CRYPTO entry.
 *       This comes before the DOI rule because an SSRN or bioRxiv preprint has a DOI
 *       too;</li>
 *   <li>a value from a record with a DOI wins;</li>
 *   <li>otherwise the longest non-blank value, counting content only: braces and
 *       line wrapping are not content, so {@code {{NEW DIRECTIONS}}} or a value split
 *       over two lines does not beat the same text written plainly;</li>
 *   <li>ties go to the earliest member.</li>
 * </ol>
 * Fields present in only one member are carried over; {@code crossref} is dropped
 * because inheritance is already applied. A chosen {@code doi} is written in its
 * normalized form, so a URL-style value never yields {@code https://doi.org/https://...}.
 *
 * <p>Entry type: the version of record's type when the cluster mixes a preprint and a
 * published version, else the most common type. Key: from {@link KeyMinter}, minted
 * from the chosen fields so it always agrees with the exported author, year and title.
 * Retired keys: every distinct member key other than the minted one.
 */
public final class GoldenRecordBuilder {

    private static final Pattern BRACES = Pattern.compile("[{}]");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final KeyMinter keyMinter;

    public GoldenRecordBuilder(KeyMinter keyMinter) {
        this.keyMinter = keyMinter;
    }

    /** @param taken keys already issued in this run; the minted key is added to it */
    public CanonicalEntry build(Cluster cluster, Set<String> taken) {
        List<NormalizedRecord> members = cluster.members();
        boolean mixed = members.stream().anyMatch(m -> m.versionRole() == VersionRole.PREPRINT)
                && members.stream().anyMatch(GoldenRecordBuilder::isVersionOfRecord);
        Map<String, String> fields = chooseFields(members, mixed);
        String entryType = chooseEntryType(members, mixed);

        SourceRecord first = members.get(0).source();
        NormalizedRecord golden = RecordNormalizer.normalize(SourceRecord.of(
                first.sourceFile(), first.line(), first.key(), entryType, fields));
        String key = keyMinter.mint(golden, taken);
        taken.add(key);

        List<String> retiredKeys = members.stream()
                .map(member -> member.source().key())
                .filter(sourceKey -> !sourceKey.equals(key))
                .distinct()
                .toList();
        List<SourceRecord> sources = members.stream().map(NormalizedRecord::source).toList();
        return new CanonicalEntry(key, entryType, fields, sources, retiredKeys);
    }

    private static Map<String, String> chooseFields(List<NormalizedRecord> members,
            boolean mixed) {
        Set<String> names = new LinkedHashSet<>();
        for (NormalizedRecord member : members) {
            names.addAll(member.source().effectiveFields().keySet());
        }
        names.remove("crossref");

        Map<String, String> chosen = new LinkedHashMap<>();
        for (String name : names) {
            NormalizedRecord winner = null;
            for (NormalizedRecord member : members) {
                // Strictly better only, so ties keep the earlier member.
                if (member.source().field(name).isPresent()
                        && (winner == null || beats(member, winner, name, mixed))) {
                    winner = member;
                }
            }
            if (winner != null) {
                chosen.put(name, name.equals("doi") && winner.ids().doi() != null
                        ? winner.ids().doi()
                        : winner.source().field(name).get());
            }
        }
        return chosen;
    }

    private static boolean beats(NormalizedRecord challenger, NormalizedRecord holder,
            String name, boolean mixed) {
        if (mixed && isVersionOfRecord(challenger) != isVersionOfRecord(holder)) {
            return isVersionOfRecord(challenger);
        }
        boolean challengerHasDoi = challenger.ids().doi() != null;
        boolean holderHasDoi = holder.ids().doi() != null;
        if (challengerHasDoi != holderHasDoi) {
            return challengerHasDoi;
        }
        return contentLength(challenger.source().field(name).get())
                > contentLength(holder.source().field(name).get());
    }

    /** Length without braces, with each whitespace run (line breaks included) as one. */
    private static int contentLength(String value) {
        String text = BRACES.matcher(value).replaceAll("");
        return WHITESPACE.matcher(text).replaceAll(" ").strip().length();
    }

    private static String chooseEntryType(List<NormalizedRecord> members, boolean mixed) {
        List<NormalizedRecord> candidates = mixed
                ? members.stream().filter(GoldenRecordBuilder::isVersionOfRecord).toList()
                : members;
        return mostCommonType(candidates).orElseThrow();
    }

    private static boolean isVersionOfRecord(NormalizedRecord record) {
        return record.versionRole() == VersionRole.VERSION_OF_RECORD;
    }

    /** The most frequent raw entry type; ties go to the type seen first. */
    private static Optional<String> mostCommonType(List<NormalizedRecord> members) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (NormalizedRecord member : members) {
            counts.merge(member.source().entryType(), 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> count : counts.entrySet()) {
            if (count.getValue() > bestCount) {
                best = count.getKey();
                bestCount = count.getValue();
            }
        }
        return Optional.ofNullable(best);
    }
}
