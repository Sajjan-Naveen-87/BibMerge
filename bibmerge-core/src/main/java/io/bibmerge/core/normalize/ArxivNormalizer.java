package io.bibmerge.core.normalize;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.bibmerge.core.model.SourceRecord;

/**
 * arXiv identifier to {@code arxiv:YYMM.NNNNN} (or {@code arxiv:archive/YYMMNNN} for
 * pre-2007 ids), version suffix stripped. Recognises the {@code eprint} field, arXiv
 * DOIs ({@code 10.48550/arXiv.*}), DBLP's {@code CoRR abs/...} volume, and ids
 * mentioned in journal, note, howpublished or url.
 */
public final class ArxivNormalizer {

    private static final String ID = "(\\d{4}\\.\\d{4,5}|[a-z][a-z\\-]+(?:\\.[a-z]{2})?/\\d{7})(?:v\\d+)?";

    private static final Pattern BARE = Pattern.compile("^(?:arxiv:)?" + ID + "$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MENTION = Pattern.compile(
            "arxiv(?:\\.org/(?:abs|pdf)/|[:\\s]+(?:preprint\\s+)?(?:arxiv:)?)" + ID,
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ARXIV_DOI = Pattern.compile("^10\\.48550/arxiv\\." + ID + "$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CORR_VOLUME = Pattern.compile("^abs/" + ID + "$",
            Pattern.CASE_INSENSITIVE);

    private ArxivNormalizer() {
    }

    /** An arXiv id from a normalized DOI such as {@code 10.48550/arxiv.1706.03762}. */
    public static Optional<String> fromDoi(String normalizedDoi) {
        return match(ARXIV_DOI, normalizedDoi);
    }

    public static Optional<String> fromRecord(SourceRecord record) {
        String archive = record.field("archiveprefix").or(() -> record.field("eprinttype"))
                .orElse("");
        Optional<String> eprint = record.field("eprint");
        if (eprint.isPresent()) {
            boolean declaredArxiv = archive.toLowerCase(Locale.ROOT).contains("arxiv");
            Optional<String> id = match(BARE, eprint.get().strip());
            if (id.isPresent() && (declaredArxiv || archive.isEmpty())) {
                return id;
            }
        }
        String journal = record.field("journal").orElse("");
        if (journal.strip().equalsIgnoreCase("corr")) {
            Optional<String> id = record.field("volume").flatMap(v -> match(CORR_VOLUME, v.strip()));
            if (id.isPresent()) {
                return id;
            }
        }
        for (String field : new String[] {"journal", "note", "howpublished", "url", "booktitle"}) {
            Optional<String> id = record.field(field).flatMap(v -> find(MENTION, v));
            if (id.isPresent()) {
                return id;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.matches() ? Optional.of(format(matcher.group(1))) : Optional.empty();
    }

    private static Optional<String> find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(format(matcher.group(1))) : Optional.empty();
    }

    private static String format(String id) {
        return "arxiv:" + id.toLowerCase(Locale.ROOT);
    }
}
