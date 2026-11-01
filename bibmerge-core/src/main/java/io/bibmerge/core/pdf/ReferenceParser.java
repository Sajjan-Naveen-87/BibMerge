package io.bibmerge.core.pdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One printed reference, as it appears in a paper's reference list, to BibTeX fields.
 *
 * <p>Identifiers (DOI, arXiv, IACR ePrint) are found reliably wherever they appear. Authors,
 * title and venue come from the shapes the common styles print:
 * <ul>
 *   <li>IEEE: {@code A. Author and B. Author, "Title," Venue, vol., pp., year.}</li>
 *   <li>Springer LNCS: {@code Author, A., Author, B.: Title. In: Venue, pp. (year)}</li>
 *   <li>plain / ACM / alpha: {@code A. Author and B. Author. Title. In Venue, pages, year.}</li>
 *   <li>Chinese GB/T 7714: {@code Family A, Family B C. Title[J]. Journal, year, vol(no): pages.}</li>
 * </ul>
 * This is a best-effort reading of printed text, not a parser of a format. The engine treats
 * records from a PDF with less trust than records from a {@code .bib} file.
 */
final class ReferenceParser {

    /** Fields under lower-case BibTeX names, and the guessed entry type. */
    record Parsed(String type, Map<String, String> fields) {
    }

    private static final Pattern DOI = Pattern.compile("(?i)(?:doi:\\s*|https?://(?:dx\\.)?doi\\.org/)?(10\\.\\d{4,9}/[^\\s,;]+)");
    private static final Pattern ARXIV = Pattern.compile(
            "(?i)(?:arxiv:\\s*|arxiv\\.org/(?:abs|pdf)/)(\\d{4}\\.\\d{4,5})(?:v\\d+)?");
    private static final Pattern EPRINT = Pattern.compile(
            "(?i)(?:cryptology\\s+e-?print\\s+archive,?\\s*(?:report|paper)?\\s*|eprint\\.iacr\\.org/)((?:19|20)\\d\\d)/(\\d{1,5})");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern YEAR = Pattern.compile("\\b((?:19|20)\\d\\d)\\b");
    private static final Pattern PAGES = Pattern.compile(
            "(?:pp\\.|pages|:)\\s*(\\d+)\\s*[\\u2013\\u2014-]+\\s*(\\d+)");
    private static final Pattern QUOTED = Pattern.compile("[\"\\u201c\\u201d](.+?)[,.]?[\"\\u201c\\u201d]");
    /** {@code Bellare, M., Rogaway, P.:} -- comma-separated "Family, I." before a colon. */
    private static final Pattern LNCS_AUTHORS = Pattern.compile(
            "^((?:[^,:.]+,\\s*(?:[A-Z][a-z]?\\.\\s?-?)+,?\\s*(?:and\\s+)?)+(?:et al\\.)?)\\s*:\\s+");
    /** GB/T 7714 document-type marker after the title: [J] journal, [C] conference, ... */
    private static final Pattern GB_MARKER = Pattern.compile("\\[(J|C|M|D|R|S|P|N|A|Z|EB/OL|J/OL|C/OL)\\]");
    /** "Durden S L": a family name followed by one to three capital initials without dots. */
    private static final Pattern GB_NAME = Pattern.compile(
            "^(\\p{Lu}[\\p{L}'-]+(?:\\s\\p{Lu}[\\p{L}'-]+)*)\\s((?:\\p{Lu}\\s?){1,3})$");
    private static final Pattern INITIALS = Pattern.compile("^(?:[A-Z][a-z]?\\.\\s?-?)+$");
    private static final Pattern PROCEEDINGS = Pattern.compile(
            "(?i)^(?:in:?\\s|proc\\.|proceedings)|conference|symposium|workshop|crypto|eurocrypt|asiacrypt");
    private static final Pattern JOURNAL = Pattern.compile(
            "(?i)journal|trans\\.|transactions|letters|\\bvol\\.|\\d+\\s*\\(\\d+\\)");

    private ReferenceParser() {
    }

    static Parsed parse(String raw) {
        String text = raw.replaceAll("\\s+", " ").strip();
        Map<String, String> fields = new LinkedHashMap<>();

        // Identifiers first; then remove them so their digits are not read as a year.
        String rest = text;
        Matcher doi = DOI.matcher(rest);
        if (doi.find()) {
            fields.put("doi", trimEnd(doi.group(1)));
            rest = rest.substring(0, doi.start()) + " " + rest.substring(doi.end());
        }
        Matcher arxiv = ARXIV.matcher(rest);
        boolean preprint = false;
        if (arxiv.find()) {
            fields.put("eprint", arxiv.group(1));
            fields.put("archiveprefix", "arXiv");
            rest = rest.substring(0, arxiv.start()) + " " + rest.substring(arxiv.end());
            preprint = true;
        }
        Matcher eprint = EPRINT.matcher(rest);
        if (eprint.find()) {
            fields.put("howpublished", "Cryptology ePrint Archive, Report " + eprint.group(1) + "/"
                    + eprint.group(2));
            rest = rest.substring(0, eprint.start()) + " " + rest.substring(eprint.end());
            preprint = true;
        }
        Matcher url = URL.matcher(rest);
        if (url.find()) {
            fields.put("url", trimEnd(url.group()));
            rest = rest.substring(0, url.start()) + " " + rest.substring(url.end());
        }

        String year = null;
        Matcher y = YEAR.matcher(rest);
        while (y.find()) {
            year = y.group(1);
        }
        Matcher pages = PAGES.matcher(rest);
        if (pages.find()) {
            fields.put("pages", pages.group(1) + "--" + pages.group(2));
        }

        // Authors, title and the venue text that follows the title.
        String authors = null;
        String title = null;
        String venue = null;
        Matcher gb = GB_MARKER.matcher(text);
        Matcher quoted = QUOTED.matcher(text);
        Matcher lncs = LNCS_AUTHORS.matcher(text);
        String gbType = null;
        int[] gbCut = gb.find() ? gbAuthorEnd(text.substring(0, gb.start())) : null;
        if (gbCut != null) {
            authors = gbAuthors(text.substring(0, gbCut[0]));
            title = text.substring(gbCut[1], gb.start()).strip();
            venue = text.substring(gb.end()).replaceAll("^[,.\\s]+", "");
            gbType = switch (gb.group(1)) {
                case "J", "J/OL" -> "article";
                case "C", "C/OL", "A" -> "inproceedings";
                case "M" -> "book";
                case "D" -> "phdthesis";
                case "R" -> "techreport";
                default -> "misc";
            };
        } else if (quoted.find()) {
            authors = text.substring(0, quoted.start()).replaceAll("[,\\s]+$", "");
            title = quoted.group(1).strip();
            venue = text.substring(quoted.end()).replaceAll("^[,.\\s]+", "");
        } else if (lncs.find()) {
            authors = lncs.group(1).strip();
            List<String> sentences = sentences(text.substring(lncs.end()));
            if (!sentences.isEmpty()) {
                title = sentences.get(0);
                venue = String.join(". ", sentences.subList(1, sentences.size()));
            }
        } else {
            List<String> sentences = sentences(text);
            if (sentences.size() >= 2 && looksLikeAuthors(sentences.get(0))) {
                authors = sentences.get(0);
                title = sentences.get(1);
                venue = String.join(". ", sentences.subList(2, sentences.size()));
                // "Title, 2001." when there is no venue: the year is not part of the title.
                title = title.replaceAll(",\\s*(?:19|20)\\d\\d$", "");
            }
        }

        if (title == null || title.isBlank()) {
            fields.put("note", text);
            if (year != null) {
                fields.put("year", year);
            }
            return new Parsed("misc", fields);
        }
        Map<String, String> ordered = new LinkedHashMap<>();
        if (authors != null && !authors.isBlank()) {
            ordered.put("author", bibtexAuthors(authors));
        }
        ordered.put("title", title.replaceAll("[.,\\s]+$", ""));
        String type = "misc";
        String place = venueName(venue);
        if (gbType != null) {
            type = gbType;
            if (place != null && !place.isBlank()) {
                ordered.put(switch (gbType) {
                    case "article" -> "journal";
                    case "inproceedings" -> "booktitle";
                    case "book", "techreport" -> "publisher";
                    case "phdthesis" -> "school";
                    default -> "howpublished";
                }, place);
            }
        } else if (!preprint && place != null && !place.isBlank()) {
            if (JOURNAL.matcher(venue).find() && !PROCEEDINGS.matcher(venue).find()) {
                type = "article";
                ordered.put("journal", place);
            } else if (PROCEEDINGS.matcher(venue).find()) {
                type = "inproceedings";
                ordered.put("booktitle", place);
            } else {
                ordered.put("howpublished", place);
            }
        }
        ordered.putAll(fields);
        if (year != null) {
            ordered.put("year", year);
        }
        return new Parsed(type, ordered);
    }

    /**
     * Splits at ". " that ends a sentence: not after a single initial ({@code M.}), not after
     * {@code pp.}, {@code vol.}, {@code no.}, {@code Proc.} or {@code eds.}. {@code et al.} does
     * end a sentence: in a reference it ends the author list.
     */
    static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '.' || (i + 1 < text.length() && text.charAt(i + 1) != ' ')) {
                continue;
            }
            int wordStart = i;
            while (wordStart > start && text.charAt(wordStart - 1) != ' ') {
                wordStart--;
            }
            String word = text.substring(wordStart, i).toLowerCase(Locale.ROOT);
            // "M." or a hyphenated "B.-M."
            boolean initial = (word.length() <= 2 || word.matches("\\p{L}\\.-\\p{L}")) && !word.isEmpty()
                    && Character.isUpperCase(text.charAt(wordStart));
            boolean abbreviation = word.matches("pp|vol|no|proc|eds?|trans|inf|comput|j|int|conf|symp|e\\.g|i\\.e|vs");
            // "In", "Proceedings" or "Journal" after a full stop always starts the venue, even
            // after a single letter such as the Roman numeral in "Part I."
            String next = text.substring(Math.min(i + 2, text.length()));
            boolean venueFollows = next.matches("(?s)(?:In[\\s:]|Proc|Journal|IEEE|ACM).*");
            if ((initial || abbreviation) && !venueFollows) {
                continue;
            }
            out.add(text.substring(start, i).strip());
            start = i + 1;
        }
        String tail = text.substring(Math.min(start, text.length())).strip();
        if (!tail.isEmpty()) {
            out.add(tail.replaceAll("\\.$", ""));
        }
        out.removeIf(String::isBlank);
        return out;
    }

    private static boolean looksLikeAuthors(String s) {
        return s.length() < 400 && (s.contains(" and ") || s.contains(",") || s.matches(".*[A-Z]\\..*")
                || s.split(" ").length <= 4);
    }

    /** "A. B, C. D, and E. F" or "B, A., D, C." to BibTeX "x and y and z". */
    static String bibtexAuthors(String raw) {
        String s = raw.strip().replaceAll("[,\\s]+$", "");
        boolean truncated = s.matches("(?i).*\\bet al\\.?$") || s.matches("(?i).*\\bet al$");
        s = s.replaceAll("(?i),?\\s*\\bet al\\.?$", "").replaceAll("(?i)\\s*&\\s*", ", ");
        List<String> tokens = new ArrayList<>();
        for (String part : s.split(",\\s*(?:and\\s+)?|\\s+and\\s+")) {
            if (!part.isBlank()) {
                tokens.add(part.strip());
            }
        }
        List<String> names = new ArrayList<>();
        boolean familyInitialPairs = tokens.size() >= 2;
        for (int i = 1; i < tokens.size(); i += 2) {
            familyInitialPairs &= INITIALS.matcher(tokens.get(i)).matches();
        }
        if (familyInitialPairs && tokens.size() % 2 == 0) {
            for (int i = 0; i < tokens.size(); i += 2) {
                names.add(tokens.get(i) + ", " + tokens.get(i + 1).strip());
            }
        } else {
            names.addAll(tokens);
        }
        if (truncated) {
            names.add("others");
        }
        return String.join(" and ", names);
    }

    private static final Pattern ET_AL_END = Pattern.compile("\\bet al\\.,?\\s+");
    private static final Pattern DOTTED_INITIAL_START = Pattern.compile("^\\p{Lu}\\.\\s");

    /**
     * {end of authors, start of title} in a GB reference's text before its [J] marker:
     * after "et al." if present; for Western dotted initials ("Y. Yamaguchi") at the first
     * real sentence end; otherwise at the first ". " (GB initials carry no dots).
     */
    private static int[] gbAuthorEnd(String prefix) {
        Matcher etAl = ET_AL_END.matcher(prefix);
        if (etAl.find()) {
            return new int[] {etAl.start() + "et al.".length(), etAl.end()};
        }
        if (DOTTED_INITIAL_START.matcher(prefix).find()) {
            List<String> parts = sentences(prefix);
            if (parts.size() >= 2) {
                int end = prefix.indexOf(parts.get(1));
                return new int[] {parts.get(0).length(), end};
            }
        }
        int cut = prefix.indexOf(". ");
        return cut > 0 ? new int[] {cut, cut + 2} : null;
    }

    /** "Freeman A, Durden S L, et al" to "Freeman, A., Durden, S. L., et al." for bibtexAuthors. */
    private static String gbAuthors(String raw) {
        List<String> out = new ArrayList<>();
        for (String token : raw.split(",\\s*")) {
            Matcher m = GB_NAME.matcher(token.strip());
            if (m.matches()) {
                out.add(m.group(1) + ", " + String.join(" ",
                        Arrays.stream(m.group(2).strip().split("\\s+")).map(i -> i + ".").toList()));
            } else {
                out.add(token.strip());
            }
        }
        return String.join(", ", out);
    }

    /** The venue name: the venue text up to pages, volume or year. */
    private static String venueName(String venue) {
        if (venue == null) {
            return null;
        }
        String v = venue.replaceAll("^(?i)in:?\\s+", "");
        v = v.split("(?i),?\\s*(?:pp?\\.\\s*\\d|pages\\b|vol\\.|no\\.|\\d+\\s*\\(\\d+\\)|\\(\\d{4}\\))"
                + "|,\\s*(?:(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\p{L}*\\.?\\s+)?(?:19|20)\\d\\d\\b"
                + "|,?\\s*(?:\\d+\\s*)?:\\s*\\d+\\s*[-\u2013]+\\s*\\d+", 2)[0];
        v = v.replaceAll("[,.]?\\s*(?:19|20)\\d\\d\\.?\\s*$", "");
        v = v.replaceAll("[,;:\\s]+$", "").strip();
        // Keep the dot of an abbreviation ("Remote Sens."), drop a sentence's full stop.
        return v.endsWith(".") && !v.matches("(?s).*\\b\\p{L}{1,5}\\.$") ? v.substring(0, v.length() - 1) : v;
    }

    private static String trimEnd(String s) {
        return s.replaceAll("[.,;)\\]]+$", "");
    }
}
