package io.bibmerge.core.normalize;

import java.util.Optional;

import io.bibmerge.core.model.Identifiers;
import io.bibmerge.core.model.SourceRecord;

/** Collects all three identifier families from one record. */
public final class IdentifierExtractor {

    private IdentifierExtractor() {
    }

    public static Identifiers extract(SourceRecord record) {
        String doi = record.field("doi").flatMap(DoiNormalizer::normalize).orElse(null);
        String arxiv = null;
        if (doi != null) {
            Optional<String> fromDoi = ArxivNormalizer.fromDoi(doi);
            if (fromDoi.isPresent()) {
                arxiv = fromDoi.get();
                doi = null;
            }
        }
        if (arxiv == null) {
            arxiv = ArxivNormalizer.fromRecord(record).orElse(null);
        }
        String iacr = IacrNormalizer.fromRecord(record).orElse(null);
        return new Identifiers(doi, arxiv, iacr);
    }
}
