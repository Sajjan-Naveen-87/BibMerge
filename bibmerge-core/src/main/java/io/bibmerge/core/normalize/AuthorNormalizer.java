package io.bibmerge.core.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import io.bibmerge.core.model.AuthorList;
import io.bibmerge.core.model.PersonName;

/**
 * BibTeX author list to {@link PersonName}s using the four-part grammar
 * {@code {given, von, family, suffix}} and all three comma forms:
 * {@code First von Last}, {@code von Last, First}, {@code von Last, Jr, First}.
 *
 * <p>A brace-wrapped name is corporate and never split. A trailing {@code others}
 * or {@code et al.} marks the list as truncated instead of becoming a person.
 */
public final class AuthorNormalizer {

    private static final Pattern ET_AL = Pattern.compile("\\s*,?\\s*et\\s*al\\.?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private AuthorNormalizer() {
    }

    public static AuthorList normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return AuthorList.EMPTY;
        }
        List<PersonName> names = new ArrayList<>();
        boolean truncated = false;
        for (String part : splitTopLevel(WHITESPACE.matcher(raw.strip()).replaceAll(" "), " and ")) {
            String name = part.strip();
            if (ET_AL.matcher(name).find()) {
                truncated = true;
                name = ET_AL.matcher(name).replaceFirst("").strip();
            }
            if (name.isEmpty()) {
                continue;
            }
            if (name.equalsIgnoreCase("others")) {
                truncated = true;
                continue;
            }
            PersonName person = parse(name);
            if (!person.family().isEmpty()) {
                names.add(person);
            }
        }
        return new AuthorList(names, truncated);
    }

    static PersonName parse(String name) {
        if (isWrappedInBraces(name)) {
            return PersonName.corporate(clean(name.substring(1, name.length() - 1)));
        }
        List<String> commaParts = splitTopLevel(name, ",");
        if (commaParts.size() == 1) {
            return firstVonLast(tokens(name));
        }
        List<String> beforeComma = tokens(commaParts.get(0));
        String suffix = commaParts.size() >= 3 ? commaParts.get(1) : "";
        String given = commaParts.size() >= 3
                ? String.join(" ", commaParts.subList(2, commaParts.size()))
                : commaParts.get(1);
        int vonEnd = 0;
        while (vonEnd < beforeComma.size() - 1 && startsLowerCase(beforeComma.get(vonEnd))) {
            vonEnd++;
        }
        return new PersonName(clean(given), clean(join(beforeComma, 0, vonEnd)),
                clean(join(beforeComma, vonEnd, beforeComma.size())), clean(suffix), false);
    }

    /** {@code First von Last}: von is the run of lower-case words before the last word. */
    private static PersonName firstVonLast(List<String> words) {
        int n = words.size();
        if (n == 1) {
            return new PersonName("", "", clean(words.get(0)), "", false);
        }
        int vonStart = -1;
        int vonEnd = -1;
        for (int i = 0; i < n - 1; i++) {
            if (startsLowerCase(words.get(i))) {
                if (vonStart < 0) {
                    vonStart = i;
                }
                vonEnd = i;
            }
        }
        if (vonStart < 0) {
            return new PersonName(clean(join(words, 0, n - 1)), "", clean(words.get(n - 1)), "",
                    false);
        }
        return new PersonName(clean(join(words, 0, vonStart)),
                clean(join(words, vonStart, vonEnd + 1)), clean(join(words, vonEnd + 1, n)), "",
                false);
    }

    private static boolean startsLowerCase(String word) {
        String decoded = Text.decode(word);
        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (Character.isLetter(c)) {
                return Character.isLowerCase(c);
            }
        }
        return false;
    }

    private static boolean isWrappedInBraces(String s) {
        if (!s.startsWith("{") || !s.endsWith("}")) {
            return false;
        }
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && i < s.length() - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String clean(String part) {
        return Text.simplify(Text.decode(part));
    }

    private static String join(List<String> words, int from, int to) {
        return String.join(" ", words.subList(from, to));
    }

    private static List<String> tokens(String s) {
        List<String> out = new ArrayList<>();
        for (String token : splitTopLevel(s.strip().replace('~', ' '), " ")) {
            if (!token.isBlank()) {
                out.add(token.strip());
            }
        }
        return out;
    }

    /** Splits on {@code separator} (case-insensitive) outside braces. */
    static List<String> splitTopLevel(String s, String separator) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        String lower = s.toLowerCase(Locale.ROOT);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0 && lower.startsWith(separator, i)) {
                parts.add(s.substring(start, i));
                i += separator.length() - 1;
                start = i + 1;
            } else if (depth == 0 && separator.equals(" ") && Character.isWhitespace(c)) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }
}
