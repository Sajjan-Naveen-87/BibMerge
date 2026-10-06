package io.bibmerge.core.normalize;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Title to its two matching forms (design doc Stage 2).
 *
 * <p>{@code full} is decoded, folded and punctuation-free. {@code key} additionally
 * drops stopwords and is used only for the deterministic key and shingling (ADR-11).
 * A trailing "(extended abstract)"-style marker is removed into a flag. "Part I" /
 * "Part II" are kept: they distinguish genuinely different works.
 */
public final class TitleNormalizer {

    public record Result(String full, String key, boolean extendedAbstract) {
    }

    private static final Pattern MARKER = Pattern.compile(
            "\\s+(?:extended abstract|full version|preliminary version|short paper"
                    + "|extended version|abstract)$");

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "of", "for", "and", "in", "on", "to", "with", "by", "from",
            "at", "as", "is", "are", "via", "its", "or");

    private TitleNormalizer() {
    }

    public static Result normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Result("", "", false);
        }
        String full = Text.simplify(Text.decode(raw));
        boolean marker = false;
        Matcher matcher = MARKER.matcher(full);
        if (matcher.find()) {
            full = full.substring(0, matcher.start()).strip();
            marker = true;
        }
        String key = Arrays.stream(full.split(" "))
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.joining(" "));
        return new Result(full, key, marker);
    }
}
