package io.bibmerge.core.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestPdfs;
import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.parse.InputParser;
import io.bibmerge.core.serialize.Dialect;

/** A paper's PDF merged with .bib files: approximate records are trusted less. */
class PdfMergeTest {

    private static final String BIB = """
            @article{dh76, author = {Whitfield Diffie and Martin E. Hellman},
              title = {New Directions in Cryptography},
              journal = {IEEE Transactions on Information Theory}, year = 1976,
              pages = {644--654}, doi = {10.1109/TIT.1976.1055638}}
            @inproceedings{BR93, author = {Mihir Bellare and Phillip Rogaway},
              title = {Random Oracles are Practical: A Paradigm for Designing Efficient Protocols},
              booktitle = {CCS}, year = 1993, pages = {62--73}}
            """;

    private static MergeResult merge(List<String> pdfLines) throws Exception {
        ParseResult bib = InputParser.parse("refs.bib", BIB.getBytes());
        ParseResult pdf = InputParser.parse("paper.pdf", TestPdfs.pdf(pdfLines));
        return MergePipeline.withDefaults(Dialect.BIBLATEX).run(List.of(bib, pdf));
    }

    @Test
    void sharedDoiMergesAndBibValuesWin() throws Exception {
        MergeResult result = merge(List.of("References",
                "[1] W. Diffie and M. Hellman, \"New directions in cryptography,\" IEEE Trans.",
                "    Inf. Theory, 1976. doi:10.1109/TIT.1976.1055638",
                "[2] A. Nobody. Something else entirely. Some Journal, 2001."));

        CanonicalEntry dh = result.entries().stream()
                .filter(e -> e.fields().get("title").contains("New Directions")).findFirst().orElseThrow();
        assertEquals(2, dh.sources().size());
        assertEquals("Whitfield Diffie and Martin E. Hellman", dh.fields().get("author"));
        assertEquals("New Directions in Cryptography", dh.fields().get("title"));
        // The PDF's made-up key is never written out as an alias.
        assertEquals(List.of("dh76"), dh.retiredKeys());
        assertTrue(!result.bib().contains("paper-ref1"), result.bib());
        assertEquals("identifier-agree", result.merges().get(0).rule());
    }

    @Test
    void exactTitleAuthorAndYearFromAPdfStillMerges() throws Exception {
        MergeResult result = merge(List.of("References",
                "[1] Bellare, M., Rogaway, P.: Random oracles are practical: A paradigm for",
                "    designing efficient protocols. In: CCS 1993, pp. 62-73. ACM (1993)"));

        assertEquals(1, result.merges().size());
        assertEquals("deterministic-key", result.merges().get(0).rule());
        // The PDF's guessed "publisher = ACM" is not added next to the .bib record.
        assertEquals(java.util.Set.of("author", "title", "booktitle", "year", "pages"),
                result.entries().stream().filter(e -> e.sources().size() == 2).findFirst()
                        .orElseThrow().fields().keySet());
    }

    /** Same paper, title printed slightly differently: a high score alone must not merge it. */
    @Test
    void fuzzyMatchFromAPdfGoesToReviewNotMerge() throws Exception {
        MergeResult result = merge(List.of("References",
                "[1] Bellare, M., Rogaway, P.: Random oracles are practical: A paradigm for",
                "    efficient protocols. In: CCS 1993, pp. 62-73. ACM (1993)"));

        assertTrue(result.merges().isEmpty(), result.merges()::toString);
        assertEquals(1, result.reviews().size());
        assertTrue(result.reviews().get(0).rule().contains("from-pdf"),
                result.reviews().get(0)::rule);
    }

    @Test
    void inputParserReadsPdfByContentNotOnlyByName() throws Exception {
        byte[] pdf = TestPdfs.pdf(List.of("References", "[1] A. B. Title here. Venue, 2000.",
                "[2] C. D. Another title. Venue, 2001."));

        ParseResult result = InputParser.parse("misnamed.bib", pdf);

        assertEquals(2, result.records().size());
    }
}
