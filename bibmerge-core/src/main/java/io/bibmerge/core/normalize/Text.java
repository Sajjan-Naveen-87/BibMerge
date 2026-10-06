package io.bibmerge.core.normalize;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import io.bibmerge.core.parse.HtmlDecoder;
import io.bibmerge.core.parse.LatexDecoder;

/** Text folding shared by the normalizers. */
public final class Text {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    private Text() {
    }

    /** Raw field text to plain Unicode: HTML entities and tags, then LaTeX. */
    public static String decode(String raw) {
        return LatexDecoder.decode(HtmlDecoder.decode(raw));
    }

    /**
     * NFKC, diacritics removed, letters with no decomposition mapped by hand,
     * lower-cased with {@link Locale#ROOT}.
     */
    public static String fold(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFKC);
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = COMBINING_MARKS.matcher(s).replaceAll("");
        s = s.replace("ß", "ss").replace("ø", "o").replace("Ø", "O")
                .replace("æ", "ae").replace("Æ", "AE").replace("œ", "oe").replace("Œ", "OE")
                .replace("ł", "l").replace("Ł", "L").replace("đ", "d").replace("Đ", "D")
                .replace("ı", "i").replace("ȷ", "j").replace("þ", "th");
        return s.toLowerCase(Locale.ROOT);
    }

    /** Folded, with every run of non-alphanumerics collapsed to one space. */
    public static String simplify(String text) {
        return NON_ALPHANUMERIC.matcher(fold(text)).replaceAll(" ").strip();
    }
}
