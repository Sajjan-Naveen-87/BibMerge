package io.bibmerge.core.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TitleNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Random Oracles are Practical: A Paradigm        | random oracles are practical a paradigm
            {Random} {O}racles are {P}ractical: a paradigm  | random oracles are practical a paradigm
            RANDOM ORACLES ARE PRACTICAL -- A PARADIGM      | random oracles are practical a paradigm
            Na\\"{\\i}ve Bayes                              | naive bayes
            The <i>k</i>-means problem                      | the k means problem
            Fully homomorphic encryption: Part {II}         | fully homomorphic encryption part ii
            """)
    void fullForm(String raw, String expected) {
        assertEquals(expected, TitleNormalizer.normalize(raw).full());
    }

    @Test
    void keyDropsStopwords() {
        assertEquals("random oracles practical paradigm designing efficient protocols",
                TitleNormalizer.normalize(
                        "Random oracles are practical: a paradigm for designing efficient protocols")
                        .key());
    }

    @Test
    void extendedAbstractMarkerBecomesAFlag() {
        TitleNormalizer.Result result = TitleNormalizer.normalize(
                "Zero-Knowledge Proofs (Extended Abstract)");

        assertEquals("zero knowledge proofs", result.full());
        assertTrue(result.extendedAbstract());
    }

    @Test
    void partNumbersAreKept() {
        assertFalse(TitleNormalizer.normalize("Lattices, Part I").full()
                .equals(TitleNormalizer.normalize("Lattices, Part II").full()));
    }

    @Test
    void blankIsEmpty() {
        assertEquals("", TitleNormalizer.normalize("  ").full());
    }
}
