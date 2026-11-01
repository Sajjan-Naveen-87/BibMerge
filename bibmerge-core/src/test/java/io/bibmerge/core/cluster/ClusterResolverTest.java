package io.bibmerge.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestRecords;
import io.bibmerge.core.match.Constraints;
import io.bibmerge.core.match.Matcher;
import io.bibmerge.core.model.CandidatePair;
import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.FeatureVector;
import io.bibmerge.core.model.IdAgreement;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;

class ClusterResolverTest {

    private static final String BIB = """
            @article{a1, title = {Same Paper}, author = {Ann Lee}, year = 2020, doi = {10.1000/x}}
            @article{b1, title = {Unrelated Work Entirely}, author = {Bo Chen}, year = 2011}
            @article{a2, title = {Same paper}, author = {Lee, A.}, year = 2020, doi = {10.1000/X}}
            @misc{a3, title = {Same Paper}, author = {Lee, Ann}, year = 2020, doi = {10.1000/x}}
            """;

    @Test
    void mergeEdgesFormClustersAndSingletonsStayAlone() {
        List<NormalizedRecord> records = TestRecords.parse("t.bib", BIB);
        Matcher.Result matched = Matcher.withDefaults().match(records);

        List<Cluster> clusters = new ClusterResolver().resolve(records, matched.merges());

        assertEquals(List.of(List.of("a1", "a2", "a3"), List.of("b1")), keys(clusters));
    }

    @Test
    void noMergesMeansOneClusterPerRecord() {
        List<NormalizedRecord> records = TestRecords.parse("t.bib", BIB);

        List<Cluster> clusters = new ClusterResolver().resolve(records, List.of());

        assertEquals(4, clusters.size());
        assertEquals("a1", clusters.get(0).members().get(0).source().key());
    }

    /**
     * a1-a2 and a2-a3 are merged, but a human says a1 and a3 are different works. The
     * chain is cut at its bottleneck edge, the one whose insertion in descending score
     * order first joins a1 and a3, and that edge goes back to review (design doc Stage 7).
     */
    @Test
    void keepSeparateCutsAChainAtItsBottleneck() {
        List<NormalizedRecord> r = TestRecords.parse("t.bib", BIB);
        NormalizedRecord a1 = r.get(0), a2 = r.get(2), a3 = r.get(3);
        PairDecision strong = merge(a2, a3, 0.99);
        PairDecision weak = merge(a1, a2, 0.90);
        Constraints c = Constraints.NONE.keepSeparate(a1.source().contentHash(),
                a3.source().contentHash());

        ClusterResolver.Resolution result = new ClusterResolver()
                .resolve(r, List.of(weak, strong), c);

        assertEquals(List.of(List.of("a1"), List.of("b1"), List.of("a2", "a3")),
                keys(result.clusters()));
        assertEquals(List.of(weak), result.cut());
    }

    @Test
    void noViolationMeansNothingIsCut() {
        List<NormalizedRecord> r = TestRecords.parse("t.bib", BIB);
        Matcher.Result matched = Matcher.withDefaults().match(r);

        ClusterResolver.Resolution result = new ClusterResolver()
                .resolve(r, matched.merges(), Constraints.NONE);

        assertEquals(List.of(List.of("a1", "a2", "a3"), List.of("b1")), keys(result.clusters()));
        assertTrue(result.cut().isEmpty());
    }

    private static PairDecision merge(NormalizedRecord left, NormalizedRecord right, double score) {
        return new PairDecision(new CandidatePair(left, right, "test"), Decision.MERGE, score,
                "test", new FeatureVector(java.util.Map.of(), IdAgreement.UNKNOWN));
    }

    private static List<List<String>> keys(List<Cluster> clusters) {
        return clusters.stream()
                .map(c -> c.members().stream().map(r -> r.source().key()).toList())
                .toList();
    }
}
