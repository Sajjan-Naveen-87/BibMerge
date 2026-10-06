package io.bibmerge.core.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.bibmerge.core.block.AllPairsBlocker;
import io.bibmerge.core.model.Decision;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.PairDecision;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.normalize.RecordNormalizer;
import io.bibmerge.core.parse.BibParser;

/** Parse → normalize → match over the two-paper synthetic corpus (see its README). */
class SyntheticCorpusTest {

    private static final List<ParseResult> PARSED = new ArrayList<>();
    private static List<NormalizedRecord> records;

    @BeforeAll
    static void load() throws IOException, URISyntaxException {
        BibParser parser = new BibParser();
        for (String name : List.of("paper-a.bib", "paper-b.bib")) {
            Path file = Path.of(SyntheticCorpusTest.class
                    .getResource("/corpus/synthetic/" + name).toURI());
            PARSED.add(parser.parse(file));
        }
        records = RecordNormalizer.normalizeAll(
                PARSED.stream().flatMap(p -> p.records().stream()).toList());
    }

    @Test
    void malformedEntryIsSkippedAndReported() {
        ParseResult paperB = PARSED.get(1);

        assertEquals(6, paperB.records().size());
        assertTrue(paperB.diagnostics().stream()
                .anyMatch(d -> d.message().startsWith("Skipped malformed")));
    }

    @Test
    void findsExactlyTheExpectedMerges() {
        Matcher.Result result = Matcher.withDefaults().match(records);

        assertEquals(Set.of(
                "DiffieH76 = dh76",
                "BR93 = DBLP:conf/ccs/BellareR93",
                "ExampleS21 = cryptoeprint:2020/123",
                "NIPS2017_attention = vaswani2017attention"),
                keys(result.merges()), () -> "reviews: " + describe(result.reviews()));
        assertTrue(result.reviews().isEmpty(), () -> describe(result.reviews()));
    }

    @Test
    void mergesHappenForTheRightReason() {
        Matcher.Result result = Matcher.withDefaults().match(records);

        assertEquals("identifier-agree", ruleFor(result, "dh76"));
        assertEquals("deterministic-key", ruleFor(result, "BR93"));
        assertEquals("identifier-agree", ruleFor(result, "vaswani2017attention"));
    }

    @Test
    void keepDistinctPolicySeparatesPreprintFromPublishedVersion() {
        Matcher strict = new Matcher(new AllPairsBlocker(), new RuleScorer(),
                new DecisionConfig(0.86, 0.62, 0.55, VersionPolicy.KEEP_DISTINCT));

        Set<String> merges = keys(strict.match(records).merges());

        assertTrue(merges.stream().noneMatch(m -> m.contains("cryptoeprint:2020/123")), merges::toString);
    }

    @Test
    void containerIsNeverComparedWithItsChild() {
        long pairs = new AllPairsBlocker().candidates(records).stream()
                .filter(p -> Set.of(p.left().source().key(), p.right().source().key())
                        .equals(Set.of("DBLP:conf/ccs/BellareR93", "DBLP:conf/ccs/1993")))
                .count();

        assertEquals(0, pairs);
    }

    private static String ruleFor(Matcher.Result result, String key) {
        return result.merges().stream()
                .filter(d -> d.pair().left().source().key().equals(key)
                        || d.pair().right().source().key().equals(key))
                .findFirst().orElseThrow().rule();
    }

    private static Set<String> keys(List<PairDecision> decisions) {
        return decisions.stream().map(d -> {
            String a = d.pair().left().source().key();
            String b = d.pair().right().source().key();
            return a.compareTo(b) < 0 ? a + " = " + b : b + " = " + a;
        }).collect(Collectors.toSet());
    }

    private static String describe(List<PairDecision> decisions) {
        return decisions.stream().map(d -> d.pair().left().source().key() + " ~ "
                + d.pair().right().source().key() + " " + d.decision() + " " + d.rule()
                + String.format(" %.3f", d.score())).collect(Collectors.joining("; "));
    }

    @Test
    void decisionsAreReportedWithEvidence() {
        Matcher.Result result = Matcher.withDefaults().match(records);

        for (PairDecision decision : result.decisions()) {
            assertTrue(decision.decision() != Decision.DISTINCT);
            assertTrue(decision.features().evidenceMass() > 0);
        }
    }
}
