package io.bibmerge.core.parse;

import java.util.List;

import io.bibmerge.core.model.ParseDiagnostic;
import io.bibmerge.core.model.SourceRecord;

/** Stage 1.5 placeholder: crossref inheritance lands in the next commit. */
final class InheritanceResolver {

    private InheritanceResolver() {
    }

    static List<SourceRecord> resolve(List<SourceRecord> records,
            List<ParseDiagnostic> diagnostics) {
        return records;
    }
}
