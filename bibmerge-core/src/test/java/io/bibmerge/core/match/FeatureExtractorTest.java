package io.bibmerge.core.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.AuthorList;
import io.bibmerge.core.model.Feature;
import io.bibmerge.core.model.FeatureVector;
import io.bibmerge.core.model.IdAgreement;
import io.bibmerge.core.model.Identifiers;
import io.bibmerge.core.model.PersonName;

class FeatureExtractorTest {

    private static PersonName person(String given, String family) {
        return new PersonName(given, "", family, "", false);
    }

    @Test
    void firstAuthorIsGraded() {
        assertEquals(1.0, FeatureExtractor.firstAuthorScore(person("mihir", "bellare"),
                person("mihir", "bellare")));
        assertEquals(0.9, FeatureExtractor.firstAuthorScore(person("mihir", "bellare"),
                person("m", "bellare")));
        assertEquals(0.6, FeatureExtractor.firstAuthorScore(person("", "bellare"),
                person("mihir", "bellare")));
        assertEquals(0.2, FeatureExtractor.firstAuthorScore(person("mihir", "bellare"),
                person("john", "bellare")));
        assertEquals(0.0, FeatureExtractor.firstAuthorScore(person("mihir", "bellare"),
                person("phillip", "rogaway")));
    }

    @Test
    void swappedNameOrderStillScores() {
        assertEquals(0.9, FeatureExtractor.firstAuthorScore(person("xiang", "li"),
                person("li", "xiang")));
    }

    @Test
    void truncatedAuthorListsUseContainment() {
        AuthorList full = new AuthorList(List.of(person("a", "vaswani"), person("n", "shazeer"),
                person("n", "parmar"), person("j", "uszkoreit")), false);
        AuthorList truncated = new AuthorList(List.of(person("a", "vaswani"),
                person("n", "shazeer")), true);
        AuthorList partial = new AuthorList(List.of(person("a", "vaswani"),
                person("n", "shazeer")), false);

        assertEquals(1.0, FeatureExtractor.authorSetScore(full, truncated));
        assertEquals(2.0 * 2 / 6, FeatureExtractor.authorSetScore(full, partial), 1e-9);
    }

    @Test
    void identifierAgreement() {
        Identifiers doiA = new Identifiers("10.1/a", null, null);
        Identifiers doiB = new Identifiers("10.1/b", null, null);
        Identifiers arxivOnly = new Identifiers(null, "arxiv:1706.03762", null);
        Identifiers doiAndArxiv = new Identifiers("10.1/a", "arxiv:1706.03762", null);

        assertEquals(IdAgreement.AGREE, FeatureExtractor.idAgreement(doiA, doiAndArxiv));
        assertEquals(IdAgreement.AGREE, FeatureExtractor.idAgreement(arxivOnly, doiAndArxiv));
        assertEquals(IdAgreement.CONFLICT, FeatureExtractor.idAgreement(doiA, doiB));
        assertEquals(IdAgreement.UNKNOWN, FeatureExtractor.idAgreement(doiA, arxivOnly));
        assertEquals(IdAgreement.UNKNOWN,
                FeatureExtractor.idAgreement(Identifiers.NONE, Identifiers.NONE));
    }

    @Test
    void jaccardOfIdenticalAndDisjointTitles() {
        assertEquals(1.0, FeatureExtractor.jaccard(FeatureExtractor.shingles("abc"),
                FeatureExtractor.shingles("abc")));
        assertEquals(0.0, FeatureExtractor.jaccard(FeatureExtractor.shingles("aaa"),
                FeatureExtractor.shingles("zzz")));
    }

    @Test
    void typeCompatibility() {
        assertEquals(1.0, TypeCompatibility.score("article", "article"));
        assertEquals(0.7, TypeCompatibility.score("article", "inproceedings"));
        assertEquals(0.7, TypeCompatibility.score("misc", "book"));
        assertEquals(0.0, TypeCompatibility.score("book", "article"));
        assertEquals(0.0, TypeCompatibility.score("proceedings", "inproceedings"));
    }

    @Test
    void ruleScorerRenormalizesOverPresentFeatures() {
        FeatureVector onlyTitle = new FeatureVector(Map.of(Feature.TITLE_JW, 0.5),
                IdAgreement.UNKNOWN);
        FeatureVector none = new FeatureVector(Map.of(), IdAgreement.UNKNOWN);

        assertEquals(0.5, new RuleScorer().score(onlyTitle), 1e-9);
        assertEquals(0.0, new RuleScorer().score(none));
        assertTrue(onlyTitle.evidenceMass() < DecisionConfig.defaults().minEvidence());
    }
}
