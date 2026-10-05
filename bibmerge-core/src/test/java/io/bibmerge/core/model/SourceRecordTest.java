package io.bibmerge.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SourceRecordTest {

    @Test
    void contentHashIgnoresKeyFileAndFieldOrder() {
        Map<String, String> ab = new LinkedHashMap<>();
        ab.put("title", "A");
        ab.put("year", "2020");
        Map<String, String> ba = new LinkedHashMap<>();
        ba.put("year", "2020");
        ba.put("title", "A");

        SourceRecord one = SourceRecord.of("a.bib", 1, "k1", "article", ab);
        SourceRecord two = SourceRecord.of("b.bib", 9, "k2", "article", ba);

        assertEquals(one.contentHash(), two.contentHash());
    }

    @Test
    void contentHashChangesWithContent() {
        SourceRecord one = SourceRecord.of("a.bib", 1, "k", "article", Map.of("title", "A"));
        SourceRecord two = SourceRecord.of("a.bib", 1, "k", "article", Map.of("title", "B"));

        assertNotEquals(one.contentHash(), two.contentHash());
    }

    @Test
    void blankFieldIsAbsent() {
        SourceRecord record = SourceRecord.of("a.bib", 1, "k", "misc", Map.of("note", "  "));

        assertTrue(record.field("note").isEmpty());
        assertTrue(record.field("missing").isEmpty());
    }

    @Test
    void inheritanceKeepsRawFieldsAndHash() {
        SourceRecord child = SourceRecord.of("a.bib", 1, "k", "inproceedings",
                Map.of("crossref", "conf"));
        SourceRecord resolved = child.withInheritance(
                Map.of("crossref", "conf", "booktitle", "Proc"), false);

        assertEquals(child.fields(), resolved.fields());
        assertEquals(child.contentHash(), resolved.contentHash());
        assertEquals("Proc", resolved.field("booktitle").orElseThrow());
    }

    @Test
    void featureVectorEvidenceMassSumsPresentWeights() {
        FeatureVector vector = new FeatureVector(
                Map.of(Feature.TITLE_JW, 1.0, Feature.YEAR_DELTA, 0.0), IdAgreement.UNKNOWN);

        assertEquals(0.29, vector.evidenceMass(), 1e-9);
        assertTrue(vector.get(Feature.VENUE_SIM).isEmpty());
    }
}
