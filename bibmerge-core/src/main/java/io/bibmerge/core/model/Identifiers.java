package io.bibmerge.core.model;

/**
 * The three identifier families (design doc Stage 2). Each value is normalized or null.
 *
 * @param doi   lower-cased, validated {@code 10.prefix/suffix}; arXiv DOIs live in {@code arxiv}
 * @param arxiv {@code arxiv:YYMM.NNNNN}, version suffix stripped
 * @param iacr  {@code iacr:YYYY/NNN}
 */
public record Identifiers(String doi, String arxiv, String iacr) {

    public static final Identifiers NONE = new Identifiers(null, null, null);

    public boolean isEmpty() {
        return doi == null && arxiv == null && iacr == null;
    }
}
