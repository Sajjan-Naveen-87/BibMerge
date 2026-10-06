package io.bibmerge.core.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestRecords;
import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.Cluster;

@Disabled("TODO(vamshi): remove once GoldenRecordBuilder is implemented")
class GoldenRecordBuilderTest {

    private final GoldenRecordBuilder builder = new GoldenRecordBuilder(new KeyMinter());

    @Test
    void combinesFieldsAndRetiresSourceKeys() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib", """
                @misc{cryptoeprint:2020/123, author = {Alice Example}, title = {Widgets},
                  howpublished = {Cryptology ePrint Archive, Report 2020/123}, year = 2020}
                @inproceedings{ExampleS21, author = {Example, Alice}, title = {Widgets},
                  booktitle = {CRYPTO 2021}, pages = {1--20}, year = 2021, doi = {10.1000/w}}
                """));
        Set<String> taken = new HashSet<>();

        CanonicalEntry entry = builder.build(cluster, taken);

        assertEquals("inproceedings", entry.entryType());
        assertEquals("10.1000/w", entry.fields().get("doi"));
        assertEquals("2021", entry.fields().get("year"));
        assertEquals("1--20", entry.fields().get("pages"));
        assertEquals(List.of("cryptoeprint:2020/123", "ExampleS21"), entry.retiredKeys());
        assertEquals(2, entry.sources().size());
        assertTrue(taken.contains(entry.key()));
    }

    @Test
    void longestValueWinsWithoutADoi() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib", """
                @article{a, author = {Lee, A.}, title = {Graphs}, journal = {J. ACM}, year = 2020}
                @article{b, author = {Ann B. Lee}, title = {Graphs}, journal = {Journal of the {ACM}},
                  year = 2020}
                """));

        CanonicalEntry entry = builder.build(cluster, new HashSet<>());

        assertEquals("Journal of the {ACM}", entry.fields().get("journal"));
        assertEquals("Ann B. Lee", entry.fields().get("author"));
    }

    @Test
    void singletonKeepsItsFields() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib",
                "@book{k, author = {Ann Lee}, title = {Graphs}, year = 2020, publisher = {P}}"));

        CanonicalEntry entry = builder.build(cluster, new HashSet<>());

        assertEquals("book", entry.entryType());
        assertEquals("P", entry.fields().get("publisher"));
        assertEquals(List.of("k"), entry.retiredKeys());
    }
}
