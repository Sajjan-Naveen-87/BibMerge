package io.bibmerge.core.normalize;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * DOI to its canonical join-key form: resolver prefix and copied punctuation removed,
 * percent-decoded, lower-cased (DOIs are case-insensitive), and shape-validated.
 * A value that fails validation is never a join key.
 */
public final class DoiNormalizer {

    private static final Pattern PREFIX = Pattern.compile(
            "^(?:https?://)?(?:dx\\.)?doi\\.org/|^doi:\\s*|^https?://[^/]+/doi/(?:abs/|full/|pdf/)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern VALID = Pattern.compile("^10\\.\\d{4,9}/\\S+$");

    private static final Pattern TRAILING = Pattern.compile("[.,;:)\\]>}'\"]+$");

    private DoiNormalizer() {
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.strip().replace("\\_", "_").replace("\\%", "%").replace("{", "")
                .replace("}", "");
        s = s.replaceAll("^[<\\[(\"']+", "");
        String previous;
        do {
            previous = s;
            s = PREFIX.matcher(s).replaceFirst("");
        } while (!s.equals(previous));
        int space = s.indexOf(' ');
        if (space > 0) {
            s = s.substring(0, space);
        }
        s = percentDecode(s);
        s = TRAILING.matcher(s).replaceAll("");
        s = s.toLowerCase(Locale.ROOT);
        return VALID.matcher(s).matches() ? Optional.of(s) : Optional.empty();
    }

    /** Decodes {@code %XX} only; a {@code +} stays a plus, unlike form decoding. */
    private static String percentDecode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1))
                    && isHex(s.charAt(i + 2))) {
                bytes.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                bytes.write(encoded, 0, encoded.length);
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }
}
