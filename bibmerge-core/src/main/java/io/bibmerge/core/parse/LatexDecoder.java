package io.bibmerge.core.parse;

import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns BibTeX field text into plain Unicode: accents, ligatures, dashes, escaped
 * specials, and braces. Math segments ({@code $...$}) are kept verbatim.
 *
 * <p>JBibTeX's own LaTeX printer is deliberately not used: it rejects a bare
 * {@code &} and silently drops some accents ({@code Erd\H{o}s} becomes "Erdos",
 * {@code \"{\i}} vanishes), and a silent change to a name changes match decisions.
 */
public final class LatexDecoder {

    private static final Pattern MATH = Pattern.compile("\\$[^$]*\\$");

    /** Accent commands mapped to Unicode combining marks. */
    private static final Map<Character, Character> ACCENTS = Map.ofEntries(
            Map.entry('\'', '́'), Map.entry('`', '̀'), Map.entry('^', '̂'),
            Map.entry('"', '̈'), Map.entry('~', '̃'), Map.entry('=', '̄'),
            Map.entry('.', '̇'), Map.entry('H', '̋'), Map.entry('c', '̧'),
            Map.entry('k', '̨'), Map.entry('v', '̌'), Map.entry('u', '̆'),
            Map.entry('r', '̊'), Map.entry('d', '̣'), Map.entry('b', '̱'));

    private static final Map<String, String> SYMBOLS = Map.ofEntries(
            Map.entry("ss", "ß"), Map.entry("o", "ø"), Map.entry("O", "Ø"),
            Map.entry("ae", "æ"), Map.entry("AE", "Æ"), Map.entry("oe", "œ"),
            Map.entry("OE", "Œ"), Map.entry("aa", "å"), Map.entry("AA", "Å"),
            Map.entry("l", "ł"), Map.entry("L", "Ł"), Map.entry("i", "ı"),
            Map.entry("j", "ȷ"), Map.entry("textendash", "–"), Map.entry("textemdash", "—"));

    /** {@code \'e}, {@code \'{e}}, {@code {\'e}}, {@code \H{o}}. */
    private static final Pattern ACCENT = Pattern.compile(
            "\\\\(['`^\"~=.]|[Hckvurdb](?![a-zA-Z]))\\s*\\{?\\s*(\\\\?[a-zA-Z])\\s*\\}?");

    private static final Pattern SYMBOL = Pattern.compile("\\\\([a-zA-Z]+)\\b\\s?(?:\\{\\})?");

    private static final Pattern COMMAND = Pattern.compile("\\\\[a-zA-Z]+\\*?\\s*");

    private LatexDecoder() {
    }

    public static String decode(String latex) {
        if (latex == null || latex.isEmpty()) {
            return "";
        }
        if (!needsDecoding(latex)) {
            return latex;
        }
        StringBuilder out = new StringBuilder();
        Matcher math = MATH.matcher(latex);
        int last = 0;
        while (math.find()) {
            out.append(decodeText(latex.substring(last, math.start()))).append(math.group());
            last = math.end();
        }
        out.append(decodeText(latex.substring(last)));
        return Normalizer.normalize(out.toString(), Normalizer.Form.NFC).strip();
    }

    private static boolean needsDecoding(String s) {
        return s.indexOf('\\') >= 0 || s.indexOf('{') >= 0 || s.indexOf('~') >= 0
                || s.contains("--");
    }

    private static String decodeText(String text) {
        String s = text.replace("---", "—").replace("--", "–");
        s = replaceAll(ACCENT, s, m -> {
            char accent = m.group(1).charAt(0);
            String base = m.group(2);
            if (base.equals("\\i")) {
                base = "i";
            } else if (base.equals("\\j")) {
                base = "j";
            }
            return base + ACCENTS.get(accent);
        });
        s = replaceAll(SYMBOL, s, m -> {
            String symbol = SYMBOLS.get(m.group(1));
            return symbol != null ? symbol : m.group();
        });
        s = s.replaceAll("(?<!\\\\)~", " ");
        s = s.replaceAll("\\\\([&%$#_{}])", "$1");
        s = COMMAND.matcher(s).replaceAll("");
        s = s.replace("{", "").replace("}", "");
        return s.replaceAll("\\s+", " ");
    }

    private static String replaceAll(Pattern pattern, String input,
            java.util.function.Function<Matcher, String> replacement) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.apply(matcher)));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
