package io.bibmerge.core.golden;

import java.util.Set;
import java.util.regex.Pattern;

import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PersonName;

/**
 * Mints {@code family + year + firstTitleWord} keys, e.g. {@code bellare1993random},
 * with a letter suffix ({@code a}, {@code b}, ...) on collision (design doc Stage 8).
 *
 * <p>Every part comes from the {@link NormalizedRecord}, which is already decoded,
 * diacritic-folded and lower-cased, so {@code Erd\H{o}s} gives {@code erdos}. Anything
 * outside {@code [a-z0-9]} is dropped, so the key is valid under classic BibTeX too.
 */
public final class KeyMinter {

    private static final Pattern NOT_KEY_SAFE = Pattern.compile("[^a-z0-9]+");

    /**
     * @param representative record whose first author, year and title form the key
     * @param taken          keys already issued; the result must not be one of them
     */
    public String mint(NormalizedRecord representative, Set<String> taken) {
        String family = keySafe(representative.authors().first().map(PersonName::family)
                .orElse(""));
        String year = representative.year() == null ? "0000"
                : String.valueOf(representative.year());
        // titleKey already has stopwords removed, so its first word is the one we want.
        String word = keySafe(representative.titleKey().split(" ", 2)[0]);

        String base = (family.isEmpty() ? "anon" : family) + year + word;
        String key = base;
        for (int n = 0; taken.contains(key); n++) {
            key = base + suffix(n);
        }
        return key;
    }

    private static String keySafe(String text) {
        return NOT_KEY_SAFE.matcher(text).replaceAll("");
    }

    /** {@code a, b, ..., z, aa, ab, ...} for n = 0, 1, ... */
    private static String suffix(int n) {
        StringBuilder letters = new StringBuilder();
        for (int i = n + 1; i > 0; i = (i - 1) / 26) {
            letters.append((char) ('a' + (i - 1) % 26));
        }
        return letters.reverse().toString();
    }
}
