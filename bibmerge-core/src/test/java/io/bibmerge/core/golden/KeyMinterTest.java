package io.bibmerge.core.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.TestRecords;
import io.bibmerge.core.model.NormalizedRecord;

class KeyMinterTest {

    private static NormalizedRecord record(String bib) {
        return TestRecords.parse("t.bib", bib).get(0);
    }

    @Test
    void familyYearFirstTitleWordSkippingStopwords() {
        NormalizedRecord r = record("@inproceedings{x, author = {Mihir Bellare and Phillip"
                + " Rogaway}, title = {The Random Oracle Methodology}, year = 1993}");

        assertEquals("bellare1993random", new KeyMinter().mint(r, Set.of()));
    }

    @Test
    void collisionsGetLetterSuffixes() {
        NormalizedRecord r = record("@article{x, author = {Ann Lee}, title = {Graphs}, year = 2020}");
        Set<String> taken = new HashSet<>(Set.of("lee2020graphs", "lee2020graphsa"));

        assertEquals("lee2020graphsb", new KeyMinter().mint(r, taken));
    }

    @Test
    void suffixesContinuePastZ() {
        NormalizedRecord r = record("@article{x, author = {Ann Lee}, title = {Graphs}, year = 2020}");
        Set<String> taken = new HashSet<>(Set.of("lee2020graphs"));
        for (char c = 'a'; c <= 'z'; c++) {
            taken.add("lee2020graphs" + c);
        }

        assertEquals("lee2020graphsaa", new KeyMinter().mint(r, taken));
    }

    @Test
    void diacriticsAndVonAreHandled() {
        NormalizedRecord r = record("@article{x, author = {Erd\\H{o}s, Paul}, title = {On Sets},"
                + " year = 1947}");

        assertEquals("erdos1947sets", new KeyMinter().mint(r, Set.of()));
    }

    @Test
    void missingPartsStillGiveAKey() {
        NormalizedRecord r = record("@misc{x, title = {Untitled Notes}}");

        assertEquals("anon0000untitled", new KeyMinter().mint(r, Set.of()));
    }
}
