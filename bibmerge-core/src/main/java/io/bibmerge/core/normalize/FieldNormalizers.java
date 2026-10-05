package io.bibmerge.core.normalize;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Venue, year, pages, volume and entry type. */
public final class FieldNormalizers {

    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d\\d|20\\d\\d)\\b");

    private static final Pattern FIRST_PAGE = Pattern.compile("^\\D*?(\\d+)");

    private static final Pattern VENUE_PREFIX = Pattern.compile(
            "^(?:in\\s+)?(?:proceedings|proc)(?:\\s+of)?(?:\\s+the)?\\s+");

    /** Aliases folded to one canonical type (design doc Stage 8). */
    private static final Map<String, String> TYPE_ALIASES = Map.of(
            "conference", "inproceedings",
            "phdthesis", "thesis",
            "mastersthesis", "thesis",
            "electronic", "online",
            "www", "online",
            "webpage", "online",
            "report", "techreport");

    private FieldNormalizers() {
    }

    /**
     * Normalized venue: decoded, folded, leading "Proceedings of the" removed.
     * Abbreviation expansion (CRYPTO, EUROCRYPT, ...) is not yet implemented.
     */
    public static String venue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return VENUE_PREFIX.matcher(Text.simplify(Text.decode(raw))).replaceFirst("").strip();
    }

    /** The first plausible four-digit year, or null. */
    public static Integer year(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(raw);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /** The first number in a page range such as {@code 123--130} or {@code e1234}. */
    public static Integer firstPage(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = FIRST_PAGE.matcher(Text.decode(raw));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String volume(String raw) {
        return raw == null ? "" : Text.simplify(Text.decode(raw));
    }

    public static String entryType(String type) {
        String lower = type.toLowerCase(Locale.ROOT);
        return TYPE_ALIASES.getOrDefault(lower, lower);
    }
}
