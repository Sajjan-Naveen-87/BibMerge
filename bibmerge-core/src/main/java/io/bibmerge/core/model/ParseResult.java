package io.bibmerge.core.model;

import java.util.List;

/**
 * Everything read from one file.
 *
 * @param sourceFile  file name
 * @param records     entries in file order
 * @param preambles   {@code @preamble} contents, re-emitted on export
 * @param diagnostics problems found, in file order
 */
public record ParseResult(
        String sourceFile,
        List<SourceRecord> records,
        List<String> preambles,
        List<ParseDiagnostic> diagnostics) {

    public ParseResult {
        records = List.copyOf(records);
        preambles = List.copyOf(preambles);
        diagnostics = List.copyOf(diagnostics);
    }
}
