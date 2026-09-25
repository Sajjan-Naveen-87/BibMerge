package io.bibmerge.core;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class BibMergeCoreTest {

    @Test
    void versionIsSet() {
        assertFalse(BibMergeCore.VERSION.isBlank());
    }
}
