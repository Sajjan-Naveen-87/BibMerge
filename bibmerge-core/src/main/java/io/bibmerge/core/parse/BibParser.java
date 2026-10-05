package io.bibmerge.core.parse;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXObject;
import org.jbibtex.BibTeXParser;
import org.jbibtex.BibTeXPreamble;
import org.jbibtex.BibTeXString;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.ReferenceValue;
import org.jbibtex.Value;

import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.ParseDiagnostic.Severity;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;

/**
 * Parses one {@code .bib} file into {@link SourceRecord}s.
 *
 * <p>The file is split at every line that starts with {@code @} and each chunk is
 * parsed on its own, so an unbalanced brace in one entry loses that entry only,
 * not the rest of the file. {@code @string} macros carry forward from earlier chunks.
 */
public final class BibParser {

    /** A chunk starts at a line whose first non-blank character is {@code @}. */
    private static final Pattern CHUNK_START = Pattern.compile("(?m)^[ \\t]*@");

    private static final Pattern LINE_IN_MESSAGE = Pattern.compile("line (\\d+)");

    /** Reads a file, decoding UTF-8 and falling back to ISO-8859-1 with a warning. */
    public ParseResult parse(Path file) throws IOException {
        return parse(file.getFileName().toString(), Files.readAllBytes(file));
    }

    public ParseResult parse(String sourceFile, byte[] content) {
        List<ParseDiagnostic> diagnostics = new ArrayList<>();
        String text = decode(sourceFile, content, diagnostics);
        return parse(sourceFile, text, diagnostics);
    }

    public ParseResult parse(String sourceFile, String text) {
        return parse(sourceFile, text, new ArrayList<>());
    }

    private ParseResult parse(String sourceFile, String text, List<ParseDiagnostic> diagnostics) {
        List<BibTeXString> macros = new ArrayList<>();
        List<SourceRecord> records = new ArrayList<>();
        List<String> preambles = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (Chunk chunk : chunks(text)) {
            LenientParser parser = LenientParser.create();
            macros.forEach(parser::addMacro);

            BibTeXDatabase database;
            try {
                database = parser.parseFully(new StringReader(chunk.text()));
            } catch (RuntimeException e) {
                diagnostics.add(new ParseDiagnostic(sourceFile, chunk.line(), Severity.ERROR,
                        "Skipped malformed entry: " + firstLine(e)));
                continue;
            }
            for (Exception e : parser.getExceptions()) {
                diagnostics.add(new ParseDiagnostic(sourceFile, lineOf(e, chunk.line()),
                        Severity.ERROR, "Skipped malformed input: " + firstLine(e)));
            }

            for (BibTeXObject object : database.getObjects()) {
                if (object instanceof BibTeXEntry entry) {
                    SourceRecord record = toRecord(sourceFile, chunk.line(), entry, diagnostics);
                    if (!seenKeys.add(record.key().toLowerCase(Locale.ROOT))) {
                        diagnostics.add(new ParseDiagnostic(sourceFile, chunk.line(),
                                Severity.WARNING, "Duplicate citation key '" + record.key()
                                        + "' in this file; both entries kept"));
                    }
                    records.add(record);
                } else if (object instanceof BibTeXString string) {
                    macros.add(string);
                } else if (object instanceof BibTeXPreamble preamble) {
                    preambles.add(userString(preamble.getValue()));
                }
            }
        }
        return new ParseResult(sourceFile, InheritanceResolver.resolve(records, diagnostics),
                preambles, diagnostics);
    }

    private static SourceRecord toRecord(String sourceFile, int line, BibTeXEntry entry,
            List<ParseDiagnostic> diagnostics) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Map.Entry<Key, Value> field : entry.getFields().entrySet()) {
            String name = field.getKey().getValue().toLowerCase(Locale.ROOT);
            Value value = field.getValue();
            if (value instanceof ReferenceValue reference && !reference.isResolved()) {
                diagnostics.add(new ParseDiagnostic(sourceFile, line, Severity.WARNING,
                        "Undefined @string '" + reference.getValue().getString() + "' in field '"
                                + name + "' of '" + entry.getKey().getValue()
                                + "'; kept as literal text"));
            }
            fields.put(name, userString(value));
        }
        return SourceRecord.of(sourceFile, line, entry.getKey().getValue(),
                entry.getType().getValue().toLowerCase(Locale.ROOT), fields);
    }

    /** Expanded text of a value; an unresolved macro falls back to its name. */
    private static String userString(Value value) {
        try {
            return value.toUserString().strip();
        } catch (RuntimeException e) {
            if (value instanceof ReferenceValue reference) {
                return reference.getValue().getString();
            }
            return "";
        }
    }

    private static String decode(String sourceFile, byte[] content,
            List<ParseDiagnostic> diagnostics) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            diagnostics.add(new ParseDiagnostic(sourceFile, 0, Severity.WARNING,
                    "File is not valid UTF-8; read as ISO-8859-1"));
            return new String(content, StandardCharsets.ISO_8859_1);
        }
    }

    private record Chunk(String text, int line) {
    }

    private static List<Chunk> chunks(String text) {
        List<Chunk> chunks = new ArrayList<>();
        Matcher matcher = CHUNK_START.matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        int line = 1;
        int lineCountedTo = 0;
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
            line += countNewlines(text, lineCountedTo, start);
            lineCountedTo = start;
            chunks.add(new Chunk(text.substring(start, end), line));
        }
        return chunks;
    }

    private static int countNewlines(String text, int from, int to) {
        int count = 0;
        for (int i = from; i < to; i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static int lineOf(Exception e, int chunkLine) {
        Matcher matcher = LINE_IN_MESSAGE.matcher(String.valueOf(e.getMessage()));
        return matcher.find() ? chunkLine + Integer.parseInt(matcher.group(1)) - 1 : chunkLine;
    }

    private static String firstLine(Throwable e) {
        Throwable root = e.getCause() != null ? e.getCause() : e;
        String message = String.valueOf(root.getMessage());
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    /** Records unresolved references as diagnostics instead of throwing. */
    private static final class LenientParser extends BibTeXParser {

        private LenientParser() throws ParseException {
            super();
        }

        static LenientParser create() {
            try {
                return new LenientParser();
            } catch (ParseException e) {
                throw new IllegalStateException("JBibTeX parser failed to initialise", e);
            }
        }

        @Override
        public void checkStringResolution(Key key, BibTeXString string) {
            // Reported per field in toRecord, where the entry and field are known.
        }

        @Override
        public void checkCrossReferenceResolution(Key key, BibTeXEntry entry) {
            // Resolved across the whole file by InheritanceResolver.
        }
    }
}
