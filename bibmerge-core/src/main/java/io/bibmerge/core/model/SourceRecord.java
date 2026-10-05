package io.bibmerge.core.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * One entry exactly as it appeared in one {@code .bib} file.
 *
 * <p>{@code fields} holds the raw values (macros expanded, LaTeX untouched, brace
 * protection intact) under lower-cased field names, in file order.
 * {@code effectiveFields} is the same map with {@code crossref} inheritance applied
 * (Stage 1.5); it equals {@code fields} for records without a parent.
 *
 * @param sourceFile      file name the record came from
 * @param line            1-based line of the {@code @type{} opener
 * @param key             citation key as written
 * @param entryType       lower-cased entry type, e.g. {@code inproceedings}
 * @param fields          raw fields, lower-cased names, file order
 * @param effectiveFields fields after inheritance
 * @param container       true when other records in the file inherit from this one
 * @param contentHash     SHA-256 over type and raw fields, independent of key and file
 */
public record SourceRecord(
        String sourceFile,
        int line,
        String key,
        String entryType,
        Map<String, String> fields,
        Map<String, String> effectiveFields,
        boolean container,
        String contentHash) {

    public SourceRecord {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(entryType, "entryType");
        Objects.requireNonNull(contentHash, "contentHash");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        effectiveFields = Collections.unmodifiableMap(new LinkedHashMap<>(effectiveFields));
    }

    /** A freshly parsed record with no inheritance applied yet. */
    public static SourceRecord of(String sourceFile, int line, String key, String entryType,
            Map<String, String> fields) {
        return new SourceRecord(sourceFile, line, key, entryType, fields, fields, false,
                hash(entryType, fields));
    }

    /** Returns a copy carrying the given inherited view and container flag. */
    public SourceRecord withInheritance(Map<String, String> inherited, boolean isContainer) {
        return new SourceRecord(sourceFile, line, key, entryType, fields, inherited, isContainer,
                contentHash);
    }

    /** An effective field value, absent when missing or blank. */
    public Optional<String> field(String name) {
        return Optional.ofNullable(effectiveFields.get(name)).filter(v -> !v.isBlank());
    }

    /** {@code file:line key}, for diagnostics and logs. */
    public String location() {
        return sourceFile + ":" + line + " " + key;
    }

    private static String hash(String entryType, Map<String, String> fields) {
        StringBuilder canonical = new StringBuilder(entryType).append('\n');
        new TreeMap<>(fields).forEach((name, value) ->
                canonical.append(name).append('=').append(value).append('\n'));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }
}
