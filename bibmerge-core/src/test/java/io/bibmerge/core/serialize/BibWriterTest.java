package io.bibmerge.core.serialize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.bibmerge.core.model.CanonicalEntry;
import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.model.SourceRecord;
import io.bibmerge.core.parse.BibParser;

class BibWriterTest {

    private static CanonicalEntry entry(String key, Map<String, String> fields,
            List<String> retired) {
        return new CanonicalEntry(key, "article", fields, List.of(), retired);
    }

    private static Map<String, String> fields(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @Test
    void writesSortedEntriesWithFixedFieldOrder() {
        String bib = new BibWriter().write(List.of(
                entry("zeta2020", fields("year", "2020", "title", "Z", "author", "Zed"), List.of()),
                entry("alpha2019", fields("journal", "J", "title", "A"), List.of())),
                List.of(), Dialect.BIBTEX);

        assertTrue(bib.indexOf("@article{alpha2019,") < bib.indexOf("@article{zeta2020,"));
        assertTrue(bib.indexOf("author = {Zed}") < bib.indexOf("title = {Z}"));
        assertTrue(bib.indexOf("title = {Z}") < bib.indexOf("year = {2020}"));
    }

    @Test
    void biblatexEmitsRetiredKeysAsIds() {
        CanonicalEntry e = entry("lee2020graphs", fields("title", "G"), List.of("a", "b"));

        assertTrue(new BibWriter().write(List.of(e), List.of(), Dialect.BIBLATEX)
                .contains("ids = {a,b}"));
        assertFalse(new BibWriter().write(List.of(e), List.of(), Dialect.BIBTEX)
                .contains("ids ="));
    }

    @Test
    void sourceIdsAreKeptAlongsideRetiredKeys() {
        CanonicalEntry e = entry("lee2020graphs", fields("title", "G", "ids", "old, a"),
                List.of("a", "b"));

        String bib = new BibWriter().write(List.of(e), List.of(), Dialect.BIBLATEX);

        assertTrue(bib.contains("ids = {a,b,old}"), bib);
        assertEquals(bib.indexOf("ids ="), bib.lastIndexOf("ids ="));
    }

    @Test
    void roundTripKeepsFieldsAndBraceProtection() {
        Map<String, String> original = fields("author", "Rivest, Ron and Shamir, Adi",
                "title", "The {RSA} Cryptosystem", "year", "1978");
        String bib = new BibWriter().write(List.of(entry("rivest1978rsa", original, List.of())),
                List.of("\\newcommand{\\noop}[1]{}"), Dialect.BIBTEX);

        ParseResult parsed = new BibParser().parse("out.bib", bib);
        SourceRecord back = parsed.records().get(0);

        assertEquals("rivest1978rsa", back.key());
        assertEquals(original, back.fields());
        assertEquals(List.of("\\newcommand{\\noop}[1]{}"), parsed.preambles());
        assertTrue(parsed.diagnostics().isEmpty());
    }
}
