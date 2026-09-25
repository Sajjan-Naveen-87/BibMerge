package io.bibmerge.core;

/**
 * Entry point marker for the merge engine.
 *
 * <p>The pipeline stages (parse, normalize, block, match, cluster, golden,
 * serialize) land in their own sub-packages per design doc §6.2.
 */
public final class BibMergeCore {

    public static final String VERSION = "0.1.0-SNAPSHOT";

    private BibMergeCore() {
    }
}
