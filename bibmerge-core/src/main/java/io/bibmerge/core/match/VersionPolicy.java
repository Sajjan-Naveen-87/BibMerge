package io.bibmerge.core.match;

/** What to do with a preprint and its version of record (ADR-9). */
public enum VersionPolicy {
    /** Merge; the golden record keeps the publisher DOI and the preprint id as eprint. */
    MERGE_WITH_VERSION_TAGS,
    /** Keep them as separate entries. */
    KEEP_DISTINCT
}
