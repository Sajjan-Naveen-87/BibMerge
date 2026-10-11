package io.bibmerge.core;

import java.util.List;

import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.normalize.RecordNormalizer;
import io.bibmerge.core.parse.BibParser;

/** Builds normalized records from inline BibTeX, for tests. */
public final class TestRecords {

    private TestRecords() {
    }

    public static List<NormalizedRecord> parse(String fileName, String bib) {
        return RecordNormalizer.normalizeAll(new BibParser().parse(fileName, bib).records());
    }
}
