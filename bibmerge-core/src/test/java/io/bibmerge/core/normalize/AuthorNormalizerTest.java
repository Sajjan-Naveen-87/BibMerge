package io.bibmerge.core.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.bibmerge.core.model.AuthorList;
import io.bibmerge.core.model.PersonName;

class AuthorNormalizerTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "-", textBlock = """
            Mihir Bellare                      | mihir         | -          | bellare    | -
            Bellare, Mihir                     | mihir         | -          | bellare    | -
            Bellare, M.                        | m             | -          | bellare    | -
            Ludwig van Beethoven               | ludwig        | van        | beethoven  | -
            van Beethoven, Ludwig              | ludwig        | van        | beethoven  | -
            Jan van der Berg                   | jan           | van der    | berg       | -
            Juan de la Cruz                    | juan          | de la      | cruz       | -
            de la Cruz, Juan                   | juan          | de la      | cruz       | -
            King, Jr., Martin Luther           | martin luther | -          | king       | jr
            Martin Luther King                 | martin luther | -          | king       | -
            Erd\\H{o}s, Paul                   | paul          | -          | erdos      | -
            J{\\"o}rg M{\\"u}ller              | jorg          | -          | muller     | -
            Plato                              | -             | -          | plato      | -
            """)
    void parsesName(String raw, String given, String von, String family, String suffix) {
        PersonName name = AuthorNormalizer.parse(raw);

        assertEquals(orEmpty(given), name.given());
        assertEquals(orEmpty(von), name.von());
        assertEquals(family, name.family());
        assertEquals(orEmpty(suffix), name.suffix());
    }

    @Test
    void corporateNameIsNeverSplit() {
        AuthorList list = AuthorNormalizer.normalize("{Barnes and Noble, Inc.} and Jane Doe");

        assertEquals(2, list.names().size());
        assertTrue(list.names().get(0).corporate());
        assertEquals("barnes and noble inc", list.names().get(0).family());
    }

    @Test
    void othersMarksTruncationNotAPerson() {
        AuthorList list = AuthorNormalizer.normalize("Shannon, C. E. and others");

        assertEquals(1, list.names().size());
        assertTrue(list.truncated());
    }

    @Test
    void etAlMarksTruncation() {
        AuthorList list = AuthorNormalizer.normalize("A. Smith et al.");

        assertEquals("smith", list.names().get(0).family());
        assertTrue(list.truncated());
    }

    @Test
    void splitsOnAndAcrossLineBreaks() {
        AuthorList list = AuthorNormalizer.normalize("Mihir Bellare\n    and Phillip Rogaway");

        List<String> families = list.names().stream().map(PersonName::family).toList();
        assertEquals(List.of("bellare", "rogaway"), families);
        assertFalse(list.truncated());
    }

    @Test
    void andInsideANameIsNotASeparator() {
        AuthorList list = AuthorNormalizer.normalize("Alexander Andersen");

        assertEquals(1, list.names().size());
        assertEquals("andersen", list.names().get(0).family());
    }

    @Test
    void emptyIsEmpty() {
        assertTrue(AuthorNormalizer.normalize("").isEmpty());
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
