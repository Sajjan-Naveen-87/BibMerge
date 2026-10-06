package io.bibmerge.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestRecords;
import io.bibmerge.core.match.Matcher;
import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.NormalizedRecord;

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

    private static List<List<String>> keys(List<Cluster> clusters) {
        return clusters.stream()
                .map(c -> c.members().stream().map(r -> r.source().key()).toList())
                .toList();
    }
}
