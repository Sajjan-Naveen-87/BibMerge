package io.bibmerge.core.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.VersionRole;
import io.bibmerge.core.parse.BibParser;

class RecordNormalizerTest {

    private static NormalizedRecord normalize(String bib) {
        return RecordNormalizer.normalize(new BibParser().parse("t.bib", bib).records().get(0));
    }

    @Test
    void normalizesEveryField() {
        NormalizedRecord record = normalize("""
                @conference{BR93,
                  author    = {Bellare, Mihir and Rogaway, Phillip},
                  title     = {Random Oracles are Practical},
                  booktitle = {Proceedings of the 1st {ACM} Conference on Computer and Communications Security},
                  year      = {1993},
                  pages     = {62--73},
                  doi       = {10.1145/168588.168596}
                }
                """);

        assertEquals("inproceedings", record.entryType());
        assertEquals("random oracles are practical", record.title());
        assertEquals("bellare", record.authors().first().orElseThrow().family());
        assertEquals("1st acm conference on computer and communications security", record.venue());
        assertEquals(1993, record.year());
        assertEquals(62, record.firstPage());
        assertEquals("10.1145/168588.168596", record.ids().doi());
        assertEquals(VersionRole.VERSION_OF_RECORD, record.versionRole());
        assertEquals("random oracles practical|bellare|1993",
                record.deterministicKey().orElseThrow());
    }

    @Test
    void yearFallsBackToBiblatexDate() {
        assertEquals(2017, normalize("@article{k, title = {T}, date = {2017-06-01}}").year());
    }

    @Test
    void missingFieldsAreEmptyNotNullStrings() {
        NormalizedRecord record = normalize("@misc{k, title = {Only a title}}");

        assertEquals("", record.venue());
        assertNull(record.year());
        assertTrue(record.authors().isEmpty());
        assertTrue(record.deterministicKey().isEmpty());
    }

    @Test
    void editorStandsInForMissingAuthor() {
        NormalizedRecord record = normalize("@book{k, editor = {Jane Doe}, title = {Edited}}");

        assertEquals("doe", record.authors().first().orElseThrow().family());
    }
}
