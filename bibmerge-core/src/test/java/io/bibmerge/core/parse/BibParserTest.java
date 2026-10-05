package io.bibmerge.core.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.ParseDiagnostic.Severity;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;

class BibParserTest {

    private final BibParser parser = new BibParser();

    @Test
    void readsTypeKeyAndLowerCasedFields() {
        ParseResult result = parser.parse("a.bib", """
                @InProceedings{BR93,
                  AUTHOR = {Mihir Bellare and Phillip Rogaway},
                  Title  = {Random Oracles are Practical},
                  year   = 1993
                }
                """);

        SourceRecord record = result.records().get(0);
        assertEquals("inproceedings", record.entryType());
        assertEquals("BR93", record.key());
        assertEquals("Random Oracles are Practical", record.field("title").orElseThrow());
        assertEquals("1993", record.field("year").orElseThrow());
        assertEquals(1, record.line());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void keepsBraceProtectionInRawFields() {
        ParseResult result = parser.parse("a.bib", "@article{k, title={The {RSA} Cryptosystem}}");

        assertEquals("The {RSA} Cryptosystem", result.records().get(0).field("title").orElseThrow());
    }

    @Test
    void expandsStringMacrosConcatenationAndMonths() {
        ParseResult result = parser.parse("a.bib", """
                @string{jacm = {Journal of the {ACM}}}
                @article{k, journal = jacm, title = "Part" # " Two", month = jan}
                """);

        SourceRecord record = result.records().get(0);
        assertEquals("Journal of the {ACM}", record.field("journal").orElseThrow());
        assertEquals("Part Two", record.field("title").orElseThrow());
        assertEquals("January", record.field("month").orElseThrow());
    }

    @Test
    void undefinedMacroIsKeptAsTextWithWarning() {
        ParseResult result = parser.parse("a.bib", "@article{k, journal = nosuchmacro}");

        assertEquals("nosuchmacro", result.records().get(0).field("journal").orElseThrow());
        assertEquals(Severity.WARNING, result.diagnostics().get(0).severity());
    }

    @Test
    void malformedEntryLosesOnlyItself() {
        ParseResult result = parser.parse("a.bib", """
                @article{good1, title = {First}}
                @article{broken, title = {never closed
                @article{good2, title = {Second}}
                """);

        List<String> keys = result.records().stream().map(SourceRecord::key).toList();
        assertEquals(List.of("good1", "good2"), keys);
        ParseDiagnostic error = result.diagnostics().get(0);
        assertEquals(Severity.ERROR, error.severity());
        assertEquals(2, error.line());
    }

    @Test
    void duplicateKeyInOneFileKeepsBothWithWarning() {
        ParseResult result = parser.parse("a.bib", """
                @article{k, title = {One}}
                @article{k, title = {Two}}
                """);

        assertEquals(2, result.records().size());
        assertTrue(result.diagnostics().get(0).message().contains("Duplicate citation key"));
    }

    @Test
    void capturesPreamble() {
        ParseResult result = parser.parse("a.bib", "@preamble{\"\\newcommand{\\noop}[1]{}\"}");

        assertEquals(List.of("\\newcommand{\\noop}[1]{}"), result.preambles());
    }

    @Test
    void emptyFileYieldsNothing() {
        ParseResult result = parser.parse("empty.bib", "");

        assertTrue(result.records().isEmpty());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void latin1FileIsReadWithWarning() {
        byte[] latin1 = "@article{k, author = {Müller, Hans}}"
                .getBytes(StandardCharsets.ISO_8859_1);

        ParseResult result = parser.parse("latin1.bib", latin1);

        assertEquals("Müller, Hans", result.records().get(0).field("author").orElseThrow());
        assertTrue(result.diagnostics().get(0).message().contains("not valid UTF-8"));
    }

    @Test
    void stripsUtf8ByteOrderMark() {
        byte[] withBom = "﻿@article{k, title = {T}}".getBytes(StandardCharsets.UTF_8);

        assertEquals("k", parser.parse("bom.bib", withBom).records().get(0).key());
    }
}
