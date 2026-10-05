package io.bibmerge.core.model;

/**
 * A problem found while reading a file. Parsing never fails the whole file for one
 * bad entry; it records a diagnostic and carries on (design doc Stage 1).
 *
 * @param sourceFile file the problem was found in
 * @param line       1-based line, or 0 when not attributable to a line
 * @param severity   {@link Severity#ERROR} when input was skipped
 * @param message    human-readable reason
 */
public record ParseDiagnostic(String sourceFile, int line, Severity severity, String message) {

    public enum Severity {
        /** Input was read, but something about it needs attention. */
        WARNING,
        /** Input was skipped. */
        ERROR
    }

    @Override
    public String toString() {
        return severity + " " + sourceFile + ":" + line + ": " + message;
    }
}
