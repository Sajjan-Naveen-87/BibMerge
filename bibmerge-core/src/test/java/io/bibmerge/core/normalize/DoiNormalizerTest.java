package io.bibmerge.core.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DoiNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            10.1145/168588.168596                          | 10.1145/168588.168596
            https://doi.org/10.1145/168588.168596          | 10.1145/168588.168596
            http://dx.doi.org/10.1145/168588.168596        | 10.1145/168588.168596
            doi:10.1145/168588.168596                      | 10.1145/168588.168596
            DOI: 10.1109/TIT.1976.1055638                  | 10.1109/tit.1976.1055638
            10.1007/978-3-540-24676-3\\_14                 | 10.1007/978-3-540-24676-3_14
            10.1007/978-3-540-24676-3%5F14                 | 10.1007/978-3-540-24676-3_14
            <10.1145/168588.168596>.                       | 10.1145/168588.168596
            {10.1145/168588.168596}                        | 10.1145/168588.168596
            https://dl.acm.org/doi/10.1145/168588.168596   | 10.1145/168588.168596
            10.1002/(SICI)1097-0258(19980815)17:15+<1661::AID-SIM968>3.0.CO;2-2 | 10.1002/(sici)1097-0258(19980815)17:15+<1661::aid-sim968>3.0.co;2-2
            """)
    void normalizes(String raw, String expected) {
        assertEquals(expected, DoiNormalizer.normalize(raw).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "n/a", "11.1145/1", "10.12/short-prefix", "doi.org/"})
    void rejectsInvalid(String raw) {
        assertTrue(DoiNormalizer.normalize(raw).isEmpty());
    }
}
