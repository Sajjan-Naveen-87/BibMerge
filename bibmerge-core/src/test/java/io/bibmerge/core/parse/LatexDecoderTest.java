package io.bibmerge.core.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LatexDecoderTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            plain text                      | plain text
            The {RSA} Cryptosystem          | The RSA Cryptosystem
            M{\\"u}ller                     | Müller
            M\\"{u}ller                     | Müller
            \\'{E}cole                      | École
            Erd\\H{o}s                      | Erdős
            Gr\\"obner~bases                | Gröbner bases
            {\\o}stergaard                  | østergaard
            Stra\\ss e                      | Straße
            pages 10--20                    | pages 10–20
            Theory \\& Practice             | Theory & Practice
            Theory & Practice               | Theory & Practice
            Na\\"{\\i}ve                    | Naïve
            Espa\\~{n}a                    | España
            \\c{C}etin                     | Çetin
            \\textit{Italic} word          | Italic word
            """)
    void decodes(String latex, String expected) {
        assertEquals(expected, LatexDecoder.decode(latex));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            On $\\mathcal{O}(n^2)$ time     | On $\\mathcal{O}(n^2)$ time
            """)
    void keepsMathVerbatim(String latex, String expected) {
        assertEquals(expected, LatexDecoder.decode(latex));
    }
}
