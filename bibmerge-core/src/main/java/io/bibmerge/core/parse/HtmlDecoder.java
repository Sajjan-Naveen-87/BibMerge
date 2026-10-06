package io.bibmerge.core.parse;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes the publisher HTML that ACM, Springer and Wiley exports carry:
 * inline tags such as {@code <i>} and {@code <sub>}, and named or numeric entities.
 */
public final class HtmlDecoder {

    private static final Pattern TAG = Pattern.compile(
            "</?(?:i|b|em|strong|sub|sup|span|scp|br|mml:[a-z]+)(?:\\s[^>]*)?/?>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);");

    private static final Map<String, String> NAMED = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"),
            Map.entry("quot", "\""), Map.entry("apos", "'"), Map.entry("nbsp", " "),
            Map.entry("ndash", "–"), Map.entry("mdash", "—"), Map.entry("hellip", "…"),
            Map.entry("lsquo", "‘"), Map.entry("rsquo", "’"), Map.entry("ldquo", "“"),
            Map.entry("rdquo", "”"));

    private HtmlDecoder() {
    }

    public static String decode(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.indexOf('<') < 0 && text.indexOf('&') < 0) {
            return text;
        }
        String withoutTags = TAG.matcher(text).replaceAll("");
        Matcher matcher = ENTITY.matcher(withoutTags);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(entity(matcher)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String entity(Matcher matcher) {
        String body = matcher.group(1);
        try {
            if (body.startsWith("#x") || body.startsWith("#X")) {
                return Character.toString(Integer.parseInt(body.substring(2), 16));
            }
            if (body.startsWith("#")) {
                return Character.toString(Integer.parseInt(body.substring(1)));
            }
        } catch (IllegalArgumentException e) {
            return matcher.group();
        }
        return NAMED.getOrDefault(body, matcher.group());
    }
}
