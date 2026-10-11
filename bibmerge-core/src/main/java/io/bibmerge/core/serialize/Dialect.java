package io.bibmerge.core.serialize;

/** Output dialect (design doc Stage 9). */
public enum Dialect {
    /** Classic BibTeX: no {@code ids} field. */
    BIBTEX,
    /** biblatex/biber: retired keys emitted as {@code ids = {...}} so old citations resolve. */
    BIBLATEX
}
