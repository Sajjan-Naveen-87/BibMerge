package io.bibmerge.core.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.match.Constraints;
import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.parse.BibParser;
import io.bibmerge.core.serialize.Dialect;

/** The mid-demo acceptance test: two papers in, one clean bibliography out. */
class MergePipelineTest {

    @Test
    void mergesTheSyntheticCorpus() throws Exception {
        List<ParseResult> inputs = new ArrayList<>();
        for (String name : List.of("paper-a.bib", "paper-b.bib")) {
            inputs.add(new BibParser().parse(Path.of(getClass()
                    .getResource("/corpus/synthetic/" + name).toURI())));
        }

        MergeResult result = MergePipeline.withDefaults(Dialect.BIBLATEX).run(inputs);

        // 12 records, 4 duplicate pairs -> 8 entries.
        assertEquals(12, result.records());
        assertEquals(8, result.entries().size());
        assertTrue(result.reviews().isEmpty());

        // The output parses cleanly and contains each merged entry exactly once.
        ParseResult reparsed = new BibParser().parse("merged.bib", result.bib());
        assertEquals(8, reparsed.records().size());
        assertTrue(reparsed.diagnostics().isEmpty());
        assertTrue(result.bib().contains("ids = {"));
    }

    /** Google Scholar-style source keys use the same shape as minted keys. */
    @Test
    void mintedKeyNeverReusesAnotherEntrysOldKey() {
        ParseResult input = new BibParser().parse("t.bib", """
                @article{lee2020graphs, author = {Bo Chen}, title = {Trees},
                  journal = {J. Trees}, year = 2019}
                @article{x1, author = {Ann Lee}, title = {Graphs of Things},
                  journal = {J. Graphs}, year = 2020}
                """);

        MergeResult result = MergePipeline.withDefaults(Dialect.BIBLATEX).run(List.of(input));

        java.util.Map<String, String> keyByTitle = new java.util.HashMap<>();
        result.entries().forEach(e -> keyByTitle.put(e.fields().get("title"), e.key()));
        assertEquals("lee2020graphsa", keyByTitle.get("Graphs of Things"));
        assertEquals(List.of("lee2020graphs"), result.entries().stream()
                .filter(e -> e.fields().get("title").equals("Trees")).findFirst().orElseThrow()
                .retiredKeys());
    }

    /**
     * Two papers both cite a different work as smith20. An ids alias on both entries
     * would make \cite{smith20} resolve to one of them silently, so neither gets it and
     * the user is told.
     */
    @Test
    void sameKeyForDifferentWorksIsNotAnAlias() {
        ParseResult a = new BibParser().parse("a.bib", """
                @article{smith20, author = {John Smith}, title = {Graph Colouring Made Easy},
                  journal = {J. Graphs}, year = 2020}
                """);
        ParseResult b = new BibParser().parse("b.bib", """
                @article{Smith20, author = {Jane Smith}, title = {Quantum Error Correction Codes},
                  journal = {Phys. Rev.}, year = 2020}
                """);

        MergeResult result = MergePipeline.withDefaults(Dialect.BIBLATEX).run(List.of(a, b));

        assertEquals(2, result.entries().size());
        result.entries().forEach(e -> assertTrue(e.retiredKeys().isEmpty(), e.key()));
        assertTrue(!result.bib().contains("ids = "), result.bib());
        assertEquals(2, result.diagnostics().stream()
                .filter(d -> d.message().contains("smith20")).count(), result.diagnostics()::toString);
    }

    /** Importing the same paper twice adds nothing (design doc §4.10, last row). */
    @Test
    void sameFileTwiceGivesTheSameEntriesOnce() {
        String bib = """
                @article{dh76, author = {Whitfield Diffie and Martin E. Hellman},
                  title = {New Directions in Cryptography}, year = 1976,
                  doi = {10.1109/TIT.1976.1055638}}
                @inproceedings{BR93, author = {Mihir Bellare and Phillip Rogaway},
                  title = {Random Oracles are Practical}, booktitle = {CCS}, year = 1993}
                """;
        ParseResult first = new BibParser().parse("paper.bib", bib);
        ParseResult again = new BibParser().parse("paper-copy.bib", bib);

        MergeResult once = MergePipeline.withDefaults(Dialect.BIBLATEX).run(List.of(first));
        MergeResult twice = MergePipeline.withDefaults(Dialect.BIBLATEX).run(List.of(first, again));

        assertEquals(2, twice.entries().size());
        assertTrue(twice.reviews().isEmpty());
        assertEquals(once.entries().stream().map(e -> e.key()).toList(),
                twice.entries().stream().map(e -> e.key()).toList());
    }

    /** Review in the browser: the reviewer's answers change the merged file. */
    @Test
    void humanDecisionsChangeTheOutput() {
        ParseResult input = new BibParser().parse("t.bib", """
                @article{dh76, author = {Whitfield Diffie and Martin E. Hellman},
                  title = {New Directions in Cryptography}, year = 1976,
                  doi = {10.1109/TIT.1976.1055638}}
                @article{DiffieH76, author = {Diffie, W. and Hellman, M.},
                  title = {{NEW DIRECTIONS IN CRYPTOGRAPHY}}, year = 1976,
                  doi = {https://doi.org/10.1109/tit.1976.1055638}}
                @inproceedings{s1, author = {Carol Fictional}, title = {Faster Sieving, Part {I}},
                  booktitle = {TCC}, year = 2019}
                @inproceedings{s2, author = {Carol Fictional}, title = {Faster Sieving, Part {II}},
                  booktitle = {TCC}, year = 2019}
                """);
        List<String> h = input.records().stream().map(r -> r.contentHash()).toList();
        MergePipeline pipeline = MergePipeline.withDefaults(Dialect.BIBLATEX);
        assertEquals(3, pipeline.run(List.of(input)).entries().size());

        MergeResult decided = pipeline.run(List.of(input),
                Constraints.NONE.keepSeparate(h.get(0), h.get(1)).merge(h.get(2), h.get(3)));

        assertEquals(3, decided.entries().size());
        assertEquals(List.of("human:merge"),
                decided.merges().stream().map(d -> d.rule()).toList());
    }

    /**
     * Adding a paper later: upload the earlier merged.bib together with the new paper.
     * Existing entries keep their keys and old aliases; nothing is duplicated.
     */
    @Test
    void addingAPaperToAnEarlierMergeKeepsKeysAndAliases() {
        ParseResult a = new BibParser().parse("a.bib", """
                @article{dh76, author = {Whitfield Diffie and Martin E. Hellman},
                  title = {New Directions in Cryptography}, journal = {IEEE Trans. Inf. Theory},
                  year = 1976, doi = {10.1109/TIT.1976.1055638}}
                """);
        ParseResult b = new BibParser().parse("b.bib", """
                @article{DiffieH76, author = {Diffie, W. and Hellman, M.},
                  title = {{NEW DIRECTIONS IN CRYPTOGRAPHY}}, year = 1976,
                  doi = {https://doi.org/10.1109/tit.1976.1055638}}
                """);
        MergePipeline pipeline = MergePipeline.withDefaults(Dialect.BIBLATEX);
        MergeResult first = pipeline.run(List.of(a, b));
        String key = first.entries().get(0).key();

        ParseResult earlier = new BibParser().parse("merged.bib", first.bib());
        ParseResult c = new BibParser().parse("c.bib", """
                @article{DH1976, author = {W. Diffie and M. Hellman},
                  title = {New directions in cryptography}, year = 1976}
                @book{knuth97, author = {Donald E. Knuth}, title = {The Art of Computer Programming},
                  publisher = {Addison-Wesley}, year = 1997}
                """);
        MergeResult second = pipeline.run(List.of(earlier, c));

        assertEquals(2, second.entries().size());
        CanonicalEntry dh = second.entries().stream()
                .filter(e -> e.key().equals(key)).findFirst().orElseThrow();
        String ids = second.bib().lines().filter(l -> l.contains("ids = ")).findFirst().orElseThrow();
        for (String old : List.of("dh76", "DiffieH76", "DH1976")) {
            assertTrue(ids.contains(old), ids);
        }
        assertTrue(dh.fields().containsKey("doi"));
    }

    @Test
    void noInputGivesAnEmptyBibliography() {
        MergeResult result = MergePipeline.withDefaults(Dialect.BIBLATEX)
                .run(List.of(new BibParser().parse("empty.bib", "")));

        assertEquals(0, result.records());
        assertTrue(result.entries().isEmpty());
        assertEquals("", result.bib());
    }

    @Test
    void entryKeepsItsOwnKeyWhenItIsAlsoTheMintedOne() {
        ParseResult input = new BibParser().parse("t.bib", """
                @article{lee2020graphs, author = {Ann Lee}, title = {Graphs}, year = 2020}
                """);

        MergeResult result = MergePipeline.withDefaults(Dialect.BIBLATEX).run(List.of(input));

        assertEquals("lee2020graphs", result.entries().get(0).key());
        assertTrue(result.entries().get(0).retiredKeys().isEmpty());
    }
}
