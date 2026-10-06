package io.bibmerge.core.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.parse.BibParser;
import io.bibmerge.core.serialize.Dialect;

/** The mid-demo acceptance test: two papers in, one clean bibliography out. */
@Disabled("TODO(vamshi): enable once stages 7-9 are implemented")
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
}
