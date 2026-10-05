package io.bibmerge.core.normalize;

import java.util.List;

import io.bibmerge.core.model.Identifiers;
import io.bibmerge.core.model.NormalizedRecord;
import io.bibmerge.core.model.SourceRecord;

/** Applies every Stage 2 normalizer to a {@link SourceRecord}. */
public final class RecordNormalizer {

    private RecordNormalizer() {
    }

    public static NormalizedRecord normalize(SourceRecord record) {
        Identifiers ids = IdentifierExtractor.extract(record);
        TitleNormalizer.Result title = TitleNormalizer.normalize(record.field("title").orElse(""));
        String venue = record.field("journal")
                .or(() -> record.field("booktitle"))
                .or(() -> record.field("howpublished"))
                .map(FieldNormalizers::venue)
                .orElse("");
        Integer year = record.field("year").map(FieldNormalizers::year)
                .orElseGet(() -> record.field("date").map(FieldNormalizers::year).orElse(null));
        return new NormalizedRecord(
                record,
                ids,
                VersionRoleDetector.detect(record, ids),
                title.full(),
                title.key(),
                title.extendedAbstract(),
                AuthorNormalizer.normalize(record.field("author")
                        .or(() -> record.field("editor")).orElse("")),
                venue,
                year,
                record.field("pages").map(FieldNormalizers::firstPage).orElse(null),
                FieldNormalizers.volume(record.field("volume").orElse(null)),
                FieldNormalizers.entryType(record.entryType()));
    }

    public static List<NormalizedRecord> normalizeAll(List<SourceRecord> records) {
        return records.stream().map(RecordNormalizer::normalize).toList();
    }
}
