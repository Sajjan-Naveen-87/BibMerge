package io.bibmerge.core.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.ParseDiagnostic.Severity;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;

/**
 * Reads the reference list of a paper's PDF into {@link SourceRecord}s.
 *
 * <p>The text is extracted with PDFBox, the last "References" or "Bibliography" heading is
 * located, and the list after it is split at entry labels ({@code [1]}, {@code [BR93]} or
 * {@code 1.}). Each entry is read by {@link ReferenceParser}. Keys are
 * {@code <file>-<label>}, so two PDFs never share a key; they are not citation keys of any
 * document, and the engine never writes them out as aliases.
 *
 * <p>Printed text loses structure, so these records are approximate. The engine trusts them
 * less than {@code .bib} records ({@link SourceRecord#fromPdf()}).
 */
public final class PdfReferenceExtractor {

    private static final Pattern HEADING = Pattern.compile(
            "(?im)^\\s*(?:\\d+\\.?\\s*)?(references|bibliography|works cited|literature cited)\\s*$");
    private static final Pattern BRACKET_LABEL = Pattern.compile("(?m)^\\s*\\[([^\\]\\s]{1,20})\\]\\s+");
    private static final Pattern NUMBER_LABEL = Pattern.compile("(?m)^\\s*(\\d{1,3})\\.\\s+");
    /** Page furniture that lands inside the list: licence stamps and bare page numbers. */
    private static final Pattern PAGE_FURNITURE = Pattern.compile(
            "(?im)^.*(?:authorized licensed use limited to|restrictions apply\\.?|downloaded on .{0,60} from ieee xplore).*$\\R?|(?m)^\\s*\\d{1,4}\\s*$\\R?");
    /** Trailing lines of a few short tokens, as left by figure labels after the list. */
    private static final Pattern TRAILING_FRAGMENTS = Pattern.compile("(?:\\R[^\\S\\r\\n]*(?:\\p{L}{1,3}[^\\S\\r\\n]*){1,3})+\\s*$");
    /** A URL broken at a line end after "/" or "-", or after "." when a lower-case part follows. */
    private static final Pattern URL_WRAP = Pattern.compile(
            "(https?:\\S*[/-])[^\\S\\r\\n]*\\R\\s*(?=\\S)|(https?:\\S*\\.)[^\\S\\r\\n]*\\R\\s*(?=\\p{Ll})");
    private static final Pattern END_OF_LIST = Pattern.compile(
            "(?im)^\\s*(?:[A-Z]\\.?\\s+)?(appendix|supplementary material)\\b.*$");

    public ParseResult parse(String fileName, byte[] content) {
        List<ParseDiagnostic> diagnostics = new ArrayList<>();
        String text;
        try (PDDocument doc = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Content order, not position order: sorting by position interleaves the two
            // columns of a two-column paper line by line.
            stripper.setSortByPosition(false);
            text = stripper.getText(doc);
        } catch (IOException | RuntimeException e) {
            diagnostics.add(new ParseDiagnostic(fileName, 0, Severity.ERROR,
                    "Could not read this PDF: " + e.getMessage()));
            return new ParseResult(fileName, List.of(), List.of(), diagnostics);
        }
        if (text.isBlank()) {
            diagnostics.add(new ParseDiagnostic(fileName, 0, Severity.WARNING,
                    "No text found in this PDF; a scanned PDF cannot be read"));
            return new ParseResult(fileName, List.of(), List.of(), diagnostics);
        }

        String section = referenceSection(text);
        if (section == null) {
            diagnostics.add(new ParseDiagnostic(fileName, 0, Severity.WARNING,
                    "No reference list found in this PDF (no \"References\" heading)"));
            return new ParseResult(fileName, List.of(), List.of(), diagnostics);
        }

        String base = baseName(fileName);
        List<SourceRecord> records = new ArrayList<>();
        List<String[]> entries = split(section);
        if (entries.isEmpty()) {
            diagnostics.add(new ParseDiagnostic(fileName, 0, Severity.WARNING,
                    "Found a reference list but could not tell where each reference starts"));
        }
        for (String[] entry : entries) {
            String label = entry[0];
            ReferenceParser.Parsed parsed = ReferenceParser.parse(entry[1]);
            String key = base + "-" + (label.matches("\\d+") ? "ref" + label : label);
            records.add(SourceRecord.of(fileName, 0, key, parsed.type(), parsed.fields()));
        }
        if (!records.isEmpty()) {
            long withTitle = records.stream().filter(r -> r.field("title").isPresent()).count();
            diagnostics.add(new ParseDiagnostic(fileName, 0, Severity.WARNING,
                    "Read " + records.size() + " references from the PDF (" + withTitle
                            + " with a title). Text from a PDF is approximate: such pairs are"
                            + " merged only on a shared identifier or an exact match, and"
                            + " otherwise sent to review"));
        }
        return new ParseResult(fileName, records, List.of(), diagnostics);
    }

    /** Text after the last references heading, up to an appendix heading if there is one. */
    static String referenceSection(String text) {
        Matcher heading = HEADING.matcher(text);
        int start = -1;
        while (heading.find()) {
            start = heading.end();
        }
        if (start < 0) {
            return null;
        }
        String section = PAGE_FURNITURE.matcher(text.substring(start)).replaceAll("");
        Matcher end = END_OF_LIST.matcher(section);
        if (end.find()) {
            section = section.substring(0, end.start());
        }
        return section;
    }

    /**
     * Entries as {label, text}. Bracket labels are used if there are any; otherwise
     * "1." numbering, accepted only while the numbers run 1, 2, 3, ... so a line starting with
     * a number inside an entry does not split it.
     */
    static List<String[]> split(String section) {
        List<int[]> starts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Matcher bracket = BRACKET_LABEL.matcher(section);
        while (bracket.find()) {
            starts.add(new int[] {bracket.start(), bracket.end()});
            labels.add(bracket.group(1));
        }
        if (starts.isEmpty()) {
            starts.clear();
            labels.clear();
            Matcher number = NUMBER_LABEL.matcher(section);
            int expected = 1;
            while (number.find()) {
                if (Integer.parseInt(number.group(1)) == expected) {
                    starts.add(new int[] {number.start(), number.end()});
                    labels.add(number.group(1));
                    expected++;
                }
            }
        }
        List<String[]> entries = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            boolean last = i + 1 == starts.size();
            int end = last ? section.length() : starts.get(i + 1)[0];
            String raw = section.substring(starts.get(i)[1], end);
            // After the last entry the text may run on into figure labels ("dP", "sP vP").
            String body = joinLines(last ? TRAILING_FRAGMENTS.matcher(raw).replaceAll("") : raw);
            if (!body.isBlank()) {
                entries.add(new String[] {labels.get(i), body});
            }
        }
        return entries;
    }

    /**
     * Re-joins URLs wrapped after "/", "-" or "." and words hyphenated at a line break
     * ("cryp-\ntography"), then unwraps lines.
     */
    static String joinLines(String s) {
        String joined;
        do {
            joined = s;
            s = URL_WRAP.matcher(s).replaceAll("$1$2");
        } while (!s.equals(joined));
        return s.replaceAll("(\\p{Ll})-\\s*\\R\\s*(\\p{Ll})", "$1$2")
                .replaceAll("\\s*\\R\\s*", " ")
                .strip();
    }

    private static String baseName(String fileName) {
        String name = fileName.replaceAll(".*[/\\\\]", "");
        int dot = name.lastIndexOf('.');
        name = dot > 0 ? name.substring(0, dot) : name;
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }
}
