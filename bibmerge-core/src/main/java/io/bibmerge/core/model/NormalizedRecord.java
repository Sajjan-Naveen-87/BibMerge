package io.bibmerge.core.model;

import java.util.Optional;

/**
 * Canonicalized view of a {@link SourceRecord}, used for matching only.
 * Absent text fields are empty strings; absent numbers are null.
 *
 * @param source           the record this was derived from
 * @param ids              normalized identifiers
 * @param versionRole      preprint, version of record, or unknown
 * @param title            full normalized title
 * @param titleKey         title with stopwords removed, for the deterministic key
 * @param extendedAbstract the title carried an "(extended abstract)"-style marker
 * @param authors          parsed authors
 * @param venue            normalized journal or booktitle
 * @param year             four-digit year, or null
 * @param firstPage        first page number, or null
 * @param volume           normalized volume, or empty
 * @param entryType        canonical entry type (aliases folded)
 */
public record NormalizedRecord(
        SourceRecord source,
        Identifiers ids,
        VersionRole versionRole,
        String title,
        String titleKey,
        boolean extendedAbstract,
        AuthorList authors,
        String venue,
        Integer year,
        Integer firstPage,
        String volume,
        String entryType) {

    /**
     * The Stage 3 deterministic key: stopword-stripped title, first-author family,
     * and year. Absent unless all three are present.
     */
    public Optional<String> deterministicKey() {
        if (titleKey.isEmpty() || year == null || authors.first().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(titleKey + "|" + authors.first().get().family() + "|" + year);
    }
}
