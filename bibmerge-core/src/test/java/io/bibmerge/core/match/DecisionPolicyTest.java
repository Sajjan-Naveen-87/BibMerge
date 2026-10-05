package io.bibmerge.core.match;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.normalize.RecordNormalizer;
import io.bibmerge.core.parse.BibParser;

class DecisionPolicyTest {

    private static PairDecision decide(String bib) {
        List<NormalizedRecord> records = RecordNormalizer.normalizeAll(
                new BibParser().parse("t.bib", bib).records());
        return Matcher.withDefaults()
                .decide(new CandidatePair(records.get(0), records.get(1), "test"));
    }

    /**
     * Shape found in real IACR ePrint exports: one ePrint record carries the DOI of its
     * published version, so it reads as a version of record. Different ePrint ids
     * still mean different papers.
     */
    @Test
    void differentEprintIdsAreDistinctEvenWhenOneCarriesADoi() {
        PairDecision d = decide("""
                @misc{cryptoeprint:2020/785, author = {Ann Lee and Bo Chen},
                  title = {The Memory-Tightness of Authenticated Encryption},
                  howpublished = {Cryptology {ePrint} Archive, Paper 2020/785}, year = {2020},
                  url = {https://eprint.iacr.org/2020/785}}
                @misc{cryptoeprint:2021/448, author = {Ann Lee and Bo Chen},
                  title = {On the Memory-Tightness of Hashed {ElGamal}},
                  howpublished = {Cryptology {ePrint} Archive, Paper 2021/448}, year = {2021},
                  doi = {10.1007/978-3-030-45724-2_2}, url = {https://eprint.iacr.org/2021/448}}
                """);

        assertEquals(Decision.DISTINCT, d.decision());
        assertEquals("veto:preprint-id-conflict", d.rule());
    }

    @Test
    void differentArxivIdsAreDistinct() {
        PairDecision d = decide("""
                @misc{a, title = {Same Title}, author = {Ann Lee}, year = 2020, eprint = {2001.00001}}
                @misc{b, title = {Same Title}, author = {Ann Lee}, year = 2020, eprint = {2002.00002}}
                """);

        assertEquals(Decision.DISTINCT, d.decision());
    }

    @Test
    void doiConflictIsExcusedForPreprintAndPublishedVersion() {
        PairDecision d = decide("""
                @misc{pre, title = {Widget Commitments}, author = {Ann Lee and Bo Chen},
                  year = 2020, howpublished = {SSRN preprint}, doi = {10.2139/ssrn.1234}}
                @article{pub, title = {Widget Commitments}, author = {Lee, Ann and Chen, Bo},
                  year = 2020, journal = {J. Widgets}, doi = {10.1000/jw.5}}
                """);

        assertEquals(Decision.MERGE, d.decision());
    }

    @Test
    void doiConflictBetweenTwoPublishedRecordsIsDistinct() {
        PairDecision d = decide("""
                @article{a, title = {Graphs}, author = {Ann Lee}, year = 2020, journal = {J},
                  doi = {10.1000/a}}
                @article{b, title = {Graphs}, author = {Ann Lee}, year = 2020, journal = {J},
                  doi = {10.1000/b}}
                """);

        assertEquals("veto:identifier-conflict", d.rule());
    }
}
