package io.bibmerge.core.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HtmlDecoderTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Theory &amp; Practice                 | Theory & Practice
            The <i>k</i>-means problem            | The k-means problem
            H<sub>2</sub>O                        | H2O
            pages 1&ndash;10                      | pages 1–10
            dash &#x2014; here                    | dash — here
            caf&#233;                             | café
            no markup                             | no markup
            &unknown; stays                       | &unknown; stays
            """)
    void decodes(String html, String expected) {
        assertEquals(expected, HtmlDecoder.decode(html));
    }
}
