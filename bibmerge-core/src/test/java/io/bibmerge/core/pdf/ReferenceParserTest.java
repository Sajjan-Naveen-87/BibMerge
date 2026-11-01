package io.bibmerge.core.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/** One printed reference string to BibTeX fields, in the styles crypto and CS papers use. */
class ReferenceParserTest {

    @Test
    void ieeeStyleWithQuotedTitle() {
        ReferenceParser.Parsed p = ReferenceParser.parse("W. Diffie and M. E. Hellman, \"New "
                + "directions in cryptography,\" IEEE Transactions on Information Theory, vol. 22, "
                + "no. 6, pp. 644-654, 1976. doi:10.1109/TIT.1976.1055638");

        assertEquals("article", p.type());
        assertEquals("W. Diffie and M. E. Hellman", p.fields().get("author"));
        assertEquals("New directions in cryptography", p.fields().get("title"));
        assertEquals("IEEE Transactions on Information Theory", p.fields().get("journal"));
        assertEquals("1976", p.fields().get("year"));
        assertEquals("644--654", p.fields().get("pages"));
        assertEquals("10.1109/TIT.1976.1055638", p.fields().get("doi"));
    }

    @Test
    void springerLncsStyle() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Bellare, M., Rogaway, P.: Random oracles "
                + "are practical: A paradigm for designing efficient protocols. In: CCS 1993, "
                + "pp. 62-73. ACM (1993)");

        assertEquals("inproceedings", p.type());
        assertEquals("Bellare, M. and Rogaway, P.", p.fields().get("author"));
        assertEquals("Random oracles are practical: A paradigm for designing efficient protocols",
                p.fields().get("title"));
        assertEquals("1993", p.fields().get("year"));
        assertEquals("62--73", p.fields().get("pages"));
    }

    @Test
    void plainStyleWithInitialsAndProceedings() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Mihir Bellare and Phillip Rogaway. Entity "
                + "authentication and key distribution. In Advances in Cryptology - CRYPTO '93, "
                + "pages 232-249, 1993.");

        assertEquals("inproceedings", p.type());
        assertEquals("Mihir Bellare and Phillip Rogaway", p.fields().get("author"));
        assertEquals("Entity authentication and key distribution", p.fields().get("title"));
        assertEquals("Advances in Cryptology - CRYPTO '93", p.fields().get("booktitle"));
        assertEquals("232--249", p.fields().get("pages"));
        assertEquals("1993", p.fields().get("year"));
    }

    @Test
    void initialsDoNotEndTheAuthorList() {
        ReferenceParser.Parsed p = ReferenceParser.parse("W. Diffie and M. E. Hellman. New directions "
                + "in cryptography. IEEE Trans. Inf. Theory, 22(6):644-654, 1976.");

        assertEquals("W. Diffie and M. E. Hellman", p.fields().get("author"));
        assertEquals("New directions in cryptography", p.fields().get("title"));
        assertEquals("article", p.type());
    }

    @Test
    void iacrEprintReference() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Alice Example and Bob Sample. Widget "
                + "commitments from lattices. Cryptology ePrint Archive, Report 2020/123, 2020. "
                + "https://eprint.iacr.org/2020/123.");

        assertEquals("misc", p.type());
        assertEquals("Widget commitments from lattices", p.fields().get("title"));
        assertEquals("Cryptology ePrint Archive, Report 2020/123", p.fields().get("howpublished"));
        assertEquals("https://eprint.iacr.org/2020/123", p.fields().get("url"));
        assertEquals("2020", p.fields().get("year"));
    }

    @Test
    void arxivReferenceWithEtAl() {
        ReferenceParser.Parsed p = ReferenceParser.parse("A. Vaswani, N. Shazeer, et al. Attention "
                + "is all you need. arXiv preprint arXiv:1706.03762v5, 2017.");

        assertEquals("A. Vaswani and N. Shazeer and others", p.fields().get("author"));
        assertEquals("Attention is all you need", p.fields().get("title"));
        assertEquals("1706.03762", p.fields().get("eprint"));
        assertEquals("arXiv", p.fields().get("archiveprefix"));
        assertEquals("2017", p.fields().get("year"));
    }

    /** "I." here is a Roman numeral ending the title, not an author's initial. */
    @Test
    void titleEndingInPartIStopsBeforeTheVenue() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Carol Fictional and Dan Placeholder. Faster "
                + "lattice sieving, part I. In Theory of Cryptography Conference, 2019.");

        assertEquals("Faster lattice sieving, part I", p.fields().get("title"));
        assertEquals("inproceedings", p.type());
        assertEquals("Theory of Cryptography Conference", p.fields().get("booktitle"));
    }

    /** Chinese GB/T 7714 style: "Family Initials", and a [J] / [C] marker after the title. */
    @Test
    void gbStyleJournalReference() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Freeman A, Durden S L. A three-component "
                + "scattering model for polarimetric SAR data[J]. IEEE Trans. Geosci. Remote Sens., "
                + "1998, 36(3): 963-973.");

        assertEquals("article", p.type());
        assertEquals("Freeman, A. and Durden, S. L.", p.fields().get("author"));
        assertEquals("A three-component scattering model for polarimetric SAR data",
                p.fields().get("title"));
        assertEquals("IEEE Trans. Geosci. Remote Sens.", p.fields().get("journal"));
        assertEquals("1998", p.fields().get("year"));
        assertEquals("963--973", p.fields().get("pages"));
    }

    @Test
    void gbStyleWithEtAlAndConference() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Cui Y, Yamaguchi Y, Yang J, et al. "
                + "Three-component power decomposition[C]. IGARSS, 2012: 100-103.");

        assertEquals("inproceedings", p.type());
        assertEquals("Cui, Y. and Yamaguchi, Y. and Yang, J. and others", p.fields().get("author"));
        assertEquals("Three-component power decomposition", p.fields().get("title"));
        assertEquals("IGARSS", p.fields().get("booktitle"));
    }

    /** Real text from an IEEE paper: GB marker, but Western initials and "et al.,". */
    @Test
    void gbMarkerWithDottedInitialsAndEtAl() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Y. Yamaguchi et al., Four-component "
                + "scattering model for polarimetric SAR image decomposition[J] IEEE Trans. Geosci. "
                + "Remote Sens., 2005, 43(8): 1699-1706");

        assertEquals("Y. Yamaguchi and others", p.fields().get("author"));
        assertEquals("Four-component scattering model for polarimetric SAR image decomposition",
                p.fields().get("title"));
        assertEquals("2005", p.fields().get("year"));
    }

    @Test
    void yearInsideAnIdentifierIsNotTheYear() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Ann Lee. Graphs. Cryptology ePrint Archive, "
                + "Report 2021/448, 2019.");

        assertEquals("2019", p.fields().get("year"));
    }

    @Test
    void textWithNoRecognisableTitleKeepsTheRawTextAsANote() {
        ReferenceParser.Parsed p = ReferenceParser.parse("Personal communication");

        assertFalse(p.fields().containsKey("title"));
        assertEquals("Personal communication", p.fields().get("note"));
    }
}
