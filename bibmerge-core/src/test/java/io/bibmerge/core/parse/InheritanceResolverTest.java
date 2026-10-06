package io.bibmerge.core.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;

class InheritanceResolverTest {

    private final BibParser parser = new BibParser();

    /** The DBLP export shape: the paper has no booktitle or year of its own. */
    private static final String DBLP = """
            @inproceedings{DBLP:conf/crypto/Paper17,
              author    = {Alice Smith and Bob Jones},
              title     = {A Paper},
              pages     = {1--20},
              crossref  = {DBLP:conf/crypto/2017-1}
            }
            @proceedings{DBLP:conf/crypto/2017-1,
              editor    = {Carol Editor},
              title     = {Advances in Cryptology - {CRYPTO} 2017},
              publisher = {Springer},
              year      = {2017}
            }
            """;

    @Test
    void childInheritsYearAndParentTitleAsBooktitle() {
        SourceRecord paper = parser.parse("dblp.bib", DBLP).records().get(0);

        assertEquals("2017", paper.field("year").orElseThrow());
        assertEquals("Advances in Cryptology - {CRYPTO} 2017",
                paper.field("booktitle").orElseThrow());
        assertEquals("A Paper", paper.field("title").orElseThrow());
        assertEquals("Springer", paper.field("publisher").orElseThrow());
    }

    @Test
    void rawFieldsAreUnchanged() {
        SourceRecord paper = parser.parse("dblp.bib", DBLP).records().get(0);

        assertFalse(paper.fields().containsKey("year"));
    }

    @Test
    void parentIsFlaggedAsContainer() {
        ParseResult result = parser.parse("dblp.bib", DBLP);

        assertFalse(result.records().get(0).container());
        assertTrue(result.records().get(1).container());
    }

    @Test
    void childFieldsWinOverParent() {
        SourceRecord paper = parser.parse("a.bib", """
                @inproceedings{child, title = {T}, year = {2016}, crossref = {parent}}
                @proceedings{parent, title = {Proc}, year = {2017}}
                """).records().get(0);

        assertEquals("2016", paper.field("year").orElseThrow());
    }

    @Test
    void crossrefKeyIsCaseInsensitive() {
        SourceRecord paper = parser.parse("a.bib", """
                @inproceedings{child, title = {T}, crossref = {PARENT}}
                @proceedings{parent, title = {Proc}, year = {2017}}
                """).records().get(0);

        assertEquals("2017", paper.field("year").orElseThrow());
    }

    @Test
    void danglingCrossrefIsReported() {
        ParseResult result = parser.parse("a.bib",
                "@inproceedings{child, title = {T}, crossref = {missing}}");

        assertTrue(result.diagnostics().get(0).message().contains("crossref 'missing'"));
        assertTrue(result.records().get(0).field("year").isEmpty());
    }
}
