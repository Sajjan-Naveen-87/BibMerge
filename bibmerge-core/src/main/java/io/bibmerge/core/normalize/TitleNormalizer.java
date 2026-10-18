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

    private static final String MARKER_PHRASES =
            "extended abstract|full version|preliminary version|short paper|extended version";
    /**
     * A trailing marker, matched before punctuation is stripped. A bare "abstract"
     * counts only in brackets: "Proofs (Abstract)" is marked, "Reasoning in the
     * Abstract" is a title that ends in a word.
     */
    private static final Pattern MARKER = Pattern.compile(
            "[\\s,.:;-]*(?:[(\\[]\\s*(?:" + MARKER_PHRASES + "|abstract)\\s*[)\\]]"
                    + "|\\b(?:" + MARKER_PHRASES + "))[\\s.]*$");

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "of", "for", "and", "in", "on", "to", "with", "by", "from",
            "at", "as", "is", "are", "via", "its", "or");

    private TitleNormalizer() {
    }

    public static Result normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Result("", "", false);
        }
        String folded = Text.fold(Text.decode(raw)).strip();
        boolean marker = false;
        Matcher matcher = MARKER.matcher(folded);
        if (matcher.find()) {
            folded = folded.substring(0, matcher.start());
            marker = true;
        }
        String full = Text.simplify(folded);
        String key = Arrays.stream(full.split(" "))
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.joining(" "));
        return new Result(full, key, marker);
    }
}
