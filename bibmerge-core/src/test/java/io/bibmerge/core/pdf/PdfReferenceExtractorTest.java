package io.bibmerge.core.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.ParseDiagnostic.Severity;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;

class PdfReferenceExtractorTest {

    static byte[] pdf(List<String> lines) throws IOException {
        return io.bibmerge.core.TestPdfs.pdf(lines);
    }

    private final PdfReferenceExtractor extractor = new PdfReferenceExtractor();

    @Test
    void bracketNumberedReferencesWrappedOverLines() throws IOException {
        byte[] bytes = pdf(List.of(
                "1 Introduction",
                "We build on key exchange [1] and random oracles [2].",
                "References",
                "[1] W. Diffie and M. E. Hellman, \"New directions in cryp-",
                "    tography,\" IEEE Transactions on Information Theory,",
                "    vol. 22, no. 6, pp. 644-654, 1976.",
                "[2] Bellare, M., Rogaway, P.: Random oracles are practical: A",
                "    paradigm for designing efficient protocols. In: CCS 1993,",
                "    pp. 62-73. ACM (1993)"));

        ParseResult result = extractor.parse("paper.pdf", bytes);

        assertEquals(2, result.records().size());
        SourceRecord first = result.records().get(0);
        assertEquals("paper-ref1", first.key());
        assertEquals("paper.pdf", first.sourceFile());
        assertEquals("New directions in cryptography", first.field("title").orElseThrow());
        assertEquals("Random oracles are practical: A paradigm for designing efficient protocols",
                result.records().get(1).field("title").orElseThrow());
        assertTrue(result.diagnostics().get(0).message().contains("2 references"),
                result.diagnostics()::toString);
    }

    @Test
    void alphabeticLabelsBecomePartOfTheKey() throws IOException {
        byte[] bytes = pdf(List.of(
                "References",
                "[BR93] Mihir Bellare and Phillip Rogaway. Entity authentication and key",
                "       distribution. In Advances in Cryptology - CRYPTO '93, pages 232-249, 1993.",
                "[DH76] W. Diffie and M. E. Hellman. New directions in cryptography. IEEE",
                "       Trans. Inf. Theory, 22(6):644-654, 1976."));

        ParseResult result = extractor.parse("survey.pdf", bytes);

        assertEquals(List.of("survey-BR93", "survey-DH76"),
                result.records().stream().map(SourceRecord::key).toList());
        assertEquals("1976", result.records().get(1).field("year").orElseThrow());
    }

    @Test
    void dotNumberedReferences() throws IOException {
        byte[] bytes = pdf(List.of(
                "Bibliography",
                "1. Bellare, M., Rogaway, P.: Random oracles are practical: A paradigm for",
                "   designing efficient protocols. In: CCS 1993, pp. 62-73. ACM (1993)",
                "2. Example, A., Sample, B.: Widget commitments from lattices. Cryptology",
                "   ePrint Archive, Report 2020/123 (2020)"));

        ParseResult result = extractor.parse("lncs.pdf", bytes);

        assertEquals(2, result.records().size());
        assertEquals("Cryptology ePrint Archive, Report 2020/123",
                result.records().get(1).field("howpublished").orElseThrow());
    }

    /** IEEE Xplore stamps a licence footer on every page; it must not end up in a reference. */
    @Test
    void pageFootersAreRemovedFromReferences() throws IOException {
        byte[] bytes = pdf(List.of(
                "6. REFERENCES",
                "[1] A. Author, \"First paper title,\" Some Journal, vol. 1,",
                "3969",
                "Authorized licensed use limited to: SOME UNIVERSITY. Downloaded on May 1,2026 at 10:00:00 UTC from IEEE Xplore. Restrictions apply.",
                "pp. 1-10, 2017.",
                "[2] B. Author, \"Second paper title,\" Other Journal, 2018."));

        ParseResult result = extractor.parse("ieee.pdf", bytes);

        assertEquals(2, result.records().size());
        assertEquals("2017", result.records().get(0).field("year").orElseThrow());
        assertTrue(result.records().get(0).field("journal").orElseThrow().startsWith("Some Journal"));
    }

    @Test
    void pdfWithoutAReferenceListIsReported() throws IOException {
        ParseResult result = extractor.parse("notes.pdf", pdf(List.of("Just some notes.")));

        assertTrue(result.records().isEmpty());
        assertEquals(Severity.WARNING, result.diagnostics().get(0).severity());
        assertTrue(result.diagnostics().get(0).message().contains("No reference list"));
    }

    @Test
    void brokenPdfIsAnErrorNotAnException() {
        ParseResult result = extractor.parse("broken.pdf",
                "%PDF-1.7 this is not really a pdf".getBytes(StandardCharsets.US_ASCII));

        assertTrue(result.records().isEmpty());
        assertEquals(Severity.ERROR, result.diagnostics().get(0).severity());
    }

    @Test
    void figureLabelsAfterTheLastEntryAreDropped() {
        List<String[]> entries = PdfReferenceExtractor.split(
                "[1] A. B. First title. Venue, 2000.\n[2] C. D. Last title. Venue,\n2001.\ndP\nsP vP\n");

        assertEquals("C. D. Last title. Venue, 2001.", entries.get(1)[1]);
    }
}
