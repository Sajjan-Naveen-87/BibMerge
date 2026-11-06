package io.bibmerge.core.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestRecords;
import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;

/** Rules 1–2 of the precedence ladder: a human decision outranks every score and veto. */
class HumanDecisionTest {

    private static final String SAME_DOI = """
            @article{dh76, author = {Whitfield Diffie and Martin E. Hellman},
              title = {New Directions in Cryptography}, year = 1976,
              doi = {10.1109/TIT.1976.1055638}}
            @article{DiffieH76, author = {Diffie, W. and Hellman, M.},
              title = {{NEW DIRECTIONS IN CRYPTOGRAPHY}}, year = 1976,
              doi = {https://doi.org/10.1109/tit.1976.1055638}}
            """;

    private static final String PARTS = """
            @inproceedings{s1, author = {Carol Fictional}, title = {Faster Sieving, Part {I}},
              booktitle = {TCC}, year = 2019}
            @inproceedings{s2, author = {Carol Fictional}, title = {Faster Sieving, Part {II}},
              booktitle = {TCC}, year = 2019}
            """;

    private static PairDecision decide(String bib, Constraints constraints) {
        List<NormalizedRecord> r = TestRecords.parse("t.bib", bib);
        return Matcher.withDefaults().decide(new CandidatePair(r.get(0), r.get(1), "test"),
                constraints);
    }

    private static String hash(String bib, int index) {
        return TestRecords.parse("t.bib", bib).get(index).source().contentHash();
    }

    @Test
    void keepSeparateBeatsAMatchingDoi() {
        Constraints c = Constraints.NONE.keepSeparate(hash(SAME_DOI, 0), hash(SAME_DOI, 1));

        PairDecision d = decide(SAME_DOI, c);

        assertEquals(Decision.DISTINCT, d.decision());
        assertEquals("human:keep-separate", d.rule());
    }

    @Test
    void mergeBeatsAVeto() {
        Constraints c = Constraints.NONE.merge(hash(PARTS, 0), hash(PARTS, 1));

        PairDecision d = decide(PARTS, c);

        assertEquals(Decision.MERGE, d.decision());
        assertEquals("human:merge", d.rule());
    }

    @Test
    void decisionIsSymmetricAndKeepSeparateWins() {
        String a = hash(SAME_DOI, 0);
        String b = hash(SAME_DOI, 1);
        Constraints c = Constraints.NONE.merge(a, b).keepSeparate(b, a);

        assertTrue(c.keepsSeparate(a, b));
        assertEquals("human:keep-separate", decide(SAME_DOI, c).rule());
    }

    @Test
    void laterDecisionReplacesEarlierOne() {
        String a = hash(SAME_DOI, 0);
        String b = hash(SAME_DOI, 1);

        Constraints c = Constraints.NONE.keepSeparate(a, b).merge(a, b);

        assertFalse(c.keepsSeparate(a, b));
        assertTrue(c.merges(a, b));
    }

    @Test
    void withoutDecisionsTheLadderIsUnchanged() {
        assertEquals("identifier-agree", decide(SAME_DOI, Constraints.NONE).rule());
        assertEquals("veto:part-number", decide(PARTS, Constraints.NONE).rule());
    }
}
