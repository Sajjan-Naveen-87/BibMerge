package io.bibmerge.core.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

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
