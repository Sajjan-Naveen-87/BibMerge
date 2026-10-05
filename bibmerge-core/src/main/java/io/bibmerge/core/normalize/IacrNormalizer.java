package io.bibmerge.core.normalize;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.bibmerge.core.model.SourceRecord;

/**
 * IACR Cryptology ePrint Archive id to {@code iacr:YYYY/NNN}.
 *
 * <p>First-class because ePrint versus proceedings is the dominant duplicate class in
 * the stakeholder's literature, often with no DOI on the ePrint side.
 */
public final class IacrNormalizer {

    private static final String ID = "((?:19|20)\\d\\d)/(\\d{1,5})";

    private static final Pattern URL = Pattern.compile("eprint\\.iacr\\.org/" + ID,
            Pattern.CASE_INSENSITIVE);

    private static final Pattern REPORT = Pattern.compile(
            "(?:cryptology\\s+e-?print\\s+archive|iacr\\s+e-?print|iacr\\s+cryptol\\.?\\s+e-?print"
                    + "\\s+arch\\.?)[^0-9]{0,40}?" + ID,
            Pattern.CASE_INSENSITIVE);

    private static final Pattern KEY = Pattern.compile("^cryptoeprint:" + ID + "$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BARE = Pattern.compile("^" + ID + "$");

    private static final Pattern IACR_CONTEXT = Pattern.compile(
            "iacr|cryptology\\s+e-?print|cryptol\\.?\\s+e-?print", Pattern.CASE_INSENSITIVE);

    private IacrNormalizer() {
    }

    public static Optional<String> fromRecord(SourceRecord record) {
        for (String field : new String[] {"url", "howpublished", "journal", "note", "booktitle",
                "institution", "publisher"}) {
            Optional<String> value = record.field(field);
            if (value.isPresent()) {
                Optional<String> id = find(URL, value.get()).or(() -> find(REPORT, value.get()));
                if (id.isPresent()) {
                    return id;
                }
            }
        }
        Optional<String> fromKey = match(KEY, record.key().strip());
        if (fromKey.isPresent()) {
            return fromKey;
        }
        Optional<String> eprint = record.field("eprint");
        if (eprint.isPresent() && mentionsIacr(record)) {
            return match(BARE, eprint.get().strip());
        }
        return Optional.empty();
    }

    private static boolean mentionsIacr(SourceRecord record) {
        for (String field : new String[] {"archiveprefix", "eprinttype", "howpublished",
                "journal", "publisher", "note", "institution"}) {
            if (record.field(field).map(v -> IACR_CONTEXT.matcher(v).find()).orElse(false)) {
                return true;
            }
        }
        return record.key().toLowerCase(Locale.ROOT).startsWith("cryptoeprint");
    }

    private static Optional<String> match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.matches() ? Optional.of(format(matcher)) : Optional.empty();
    }

    private static Optional<String> find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(format(matcher)) : Optional.empty();
    }

    private static String format(Matcher matcher) {
        return "iacr:" + matcher.group(1) + "/"
                + String.format(Locale.ROOT, "%03d", Integer.parseInt(matcher.group(2)));
    }
}
