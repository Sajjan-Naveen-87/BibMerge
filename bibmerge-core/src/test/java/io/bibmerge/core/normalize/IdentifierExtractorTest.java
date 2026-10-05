package io.bibmerge.core.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.Identifiers;
import io.bibmerge.core.model.SourceRecord;
import io.bibmerge.core.model.VersionRole;

class IdentifierExtractorTest {

    private static SourceRecord record(String key, String type, Map<String, String> fields) {
        return SourceRecord.of("t.bib", 1, key, type, fields);
    }

    private static Identifiers ids(Map<String, String> fields) {
        return IdentifierExtractor.extract(record("k", "misc", fields));
    }

    @Test
    void arxivFromEprintFieldDropsVersion() {
        assertEquals("arxiv:1706.03762",
                ids(Map.of("eprint", "1706.03762v5", "archiveprefix", "arXiv")).arxiv());
    }

    @Test
    void arxivFromArxivDoiIsNotKeptAsDoi() {
        Identifiers ids = ids(Map.of("doi", "10.48550/arXiv.1706.03762"));

        assertEquals("arxiv:1706.03762", ids.arxiv());
        assertNull(ids.doi());
    }

    @Test
    void arxivFromJournalMention() {
        assertEquals("arxiv:2101.00001",
                ids(Map.of("journal", "arXiv preprint arXiv:2101.00001")).arxiv());
    }

    @Test
    void arxivFromDblpCorrVolume() {
        assertEquals("arxiv:1702.01234",
                ids(Map.of("journal", "CoRR", "volume", "abs/1702.01234")).arxiv());
    }

    @Test
    void arxivOldStyleFromUrl() {
        assertEquals("arxiv:hep-th/9901001",
                ids(Map.of("url", "https://arxiv.org/abs/hep-th/9901001v2")).arxiv());
    }

    @Test
    void iacrFromUrl() {
        assertEquals("iacr:2019/042", ids(Map.of("url", "https://eprint.iacr.org/2019/42")).iacr());
    }

    @Test
    void iacrFromHowpublishedReport() {
        assertEquals("iacr:2020/1234", ids(Map.of("howpublished",
                "Cryptology ePrint Archive, Report 2020/1234")).iacr());
    }

    @Test
    void iacrFromCryptoeprintKeyConvention() {
        Identifiers ids = IdentifierExtractor.extract(
                record("cryptoeprint:2021/555", "misc", Map.of("title", "T")));

        assertEquals("iacr:2021/555", ids.iacr());
    }

    @Test
    void iacrFromEprintFieldOnlyWithIacrContext() {
        assertEquals("iacr:2018/100", ids(Map.of("eprint", "2018/100",
                "howpublished", "Cryptology ePrint Archive")).iacr());
        assertNull(ids(Map.of("eprint", "2018/100")).iacr());
    }

    @Test
    void invalidDoiIsDropped() {
        assertNull(ids(Map.of("doi", "not a doi")).doi());
    }

    @Test
    void versionRoles() {
        SourceRecord published = record("a", "article",
                Map.of("journal", "Journal of the ACM", "title", "T"));
        SourceRecord preprint = record("b", "misc",
                Map.of("howpublished", "Cryptology ePrint Archive, Report 2020/1"));
        SourceRecord withDoi = record("c", "misc", Map.of("doi", "10.1145/1.2"));
        SourceRecord bare = record("d", "misc", Map.of("title", "T"));

        assertEquals(VersionRole.VERSION_OF_RECORD, role(published));
        assertEquals(VersionRole.PREPRINT, role(preprint));
        assertEquals(VersionRole.VERSION_OF_RECORD, role(withDoi));
        assertEquals(VersionRole.UNKNOWN, role(bare));
    }

    @Test
    void corrJournalIsPreprint() {
        SourceRecord corr = record("e", "article", Map.of("journal", "CoRR",
                "volume", "abs/1702.01234"));

        assertEquals(VersionRole.PREPRINT, role(corr));
    }

    private static VersionRole role(SourceRecord record) {
        return VersionRoleDetector.detect(record, IdentifierExtractor.extract(record));
    }
}
