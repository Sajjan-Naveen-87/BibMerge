package io.bibmerge.core.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestRecords;
import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.Cluster;
import io.bibmerge.core.model.NormalizedRecord;

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
    void bracesAndLineBreaksDoNotCountAsLength() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib", """
                @article{a, author = {Ann Lee and Bo Chen}, title = {New Directions}, year = 2020}
                @article{b, author = {Ann Lee and\r
                             Bo Chen}, title = {{NEW DIRECTIONS}}, year = 2020}
                """));

        CanonicalEntry entry = builder.build(cluster, new HashSet<>());

        assertEquals("Ann Lee and Bo Chen", entry.fields().get("author"));
        assertEquals("New Directions", entry.fields().get("title"));
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

    @Test
    void keyIsMintedFromTheChosenFields() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib", """
                @misc{e, author = {Alice Example}, title = {Widgets}, year = 2020,
                  howpublished = {Cryptology ePrint Archive, Report 2020/123}}
                @inproceedings{p, author = {Example, Alice}, title = {Widgets}, year = 2021,
                  doi = {10.1000/w}}
                """));

        CanonicalEntry entry = builder.build(cluster, new HashSet<>());

        // The DOI record's year wins, and the key says the same year.
        assertEquals("example2021widgets", entry.key());
    }

    @Test
    void doiIsWrittenInNormalizedForm() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib", """
                @article{a, title = {T}, doi = {10.1109/TIT.1976.1055638}}
                @article{b, title = {T}, doi = {https://doi.org/10.1109/tit.1976.1055638}}
                """));

        CanonicalEntry entry = builder.build(cluster, new HashSet<>());

        assertEquals("10.1109/tit.1976.1055638", entry.fields().get("doi"));
    }

    @Test
    void inheritedFieldsAreKeptAndCrossrefIsDropped() {
        NormalizedRecord child = TestRecords.parse("t.bib", """
                @inproceedings{c, author = {Ann Lee}, title = {Graphs}, crossref = {proc}}
                @proceedings{proc, title = {Proceedings of Graphs 2020}, year = 2020}
                """).get(0);

        CanonicalEntry entry = builder.build(new Cluster(List.of(child)), new HashSet<>());

        assertEquals("Proceedings of Graphs 2020", entry.fields().get("booktitle"));
        assertEquals("2020", entry.fields().get("year"));
        assertFalse(entry.fields().containsKey("crossref"));
    }

    @Test
    void retiredKeysSkipTheMintedKeyAndRepeats() {
        Cluster cluster = new Cluster(TestRecords.parse("t.bib", """
                @article{lee2020graphs, author = {Ann Lee}, title = {Graphs}, year = 2020}
                @article{lee2020graphs, author = {Lee, A.}, title = {Graphs}, year = 2020}
                @article{LeeGraphs, author = {Lee, Ann}, title = {Graphs}, year = 2020}
                """));

        CanonicalEntry entry = builder.build(cluster, new HashSet<>());

        assertEquals("lee2020graphs", entry.key());
        assertEquals(List.of("LeeGraphs"), entry.retiredKeys());
    }
}
