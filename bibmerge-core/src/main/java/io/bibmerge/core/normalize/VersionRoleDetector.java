package io.bibmerge.core.normalize;

import java.util.Locale;
import java.util.regex.Pattern;

import io.bibmerge.core.model.Identifiers;
import io.bibmerge.core.model.SourceRecord;
import io.bibmerge.core.model.VersionRole;

/** Preprint, version of record, or unknown (design doc Stage 2). */
public final class VersionRoleDetector {

    private static final Pattern PREPRINT_VENUE = Pattern.compile(
            "\\b(?:arxiv|corr|e-?print|preprint|ssrn|biorxiv|medrxiv)\\b",
            Pattern.CASE_INSENSITIVE);

    private VersionRoleDetector() {
    }

    public static VersionRole detect(SourceRecord record, Identifiers ids) {
        if (ids.doi() != null) {
            return VersionRole.VERSION_OF_RECORD;
        }
        String venue = record.field("journal").or(() -> record.field("booktitle")).orElse("");
        if (!venue.isBlank() && !PREPRINT_VENUE.matcher(venue).find()) {
            return VersionRole.VERSION_OF_RECORD;
        }
        if (ids.arxiv() != null || ids.iacr() != null || PREPRINT_VENUE.matcher(venue).find()
                || record.field("howpublished").map(v -> PREPRINT_VENUE.matcher(v).find())
                        .orElse(false)
                || record.field("note").map(v -> v.toLowerCase(Locale.ROOT).contains("preprint"))
                        .orElse(false)) {
            return VersionRole.PREPRINT;
        }
        return VersionRole.UNKNOWN;
    }
}
