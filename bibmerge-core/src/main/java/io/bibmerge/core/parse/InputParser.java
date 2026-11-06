package io.bibmerge.core.parse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import io.bibmerge.core.model.ParseResult;
import io.bibmerge.core.pdf.PdfReferenceExtractor;

/**
 * Reads one input file: a {@code .bib} file with {@link BibParser}, or a paper's PDF with
 * {@link PdfReferenceExtractor}. A PDF is recognised by its {@code %PDF-} header as well as
 * its name, and its records always carry a {@code .pdf} file name so the engine knows they
 * came from printed text.
 */
public final class InputParser {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private InputParser() {
    }

    public static ParseResult parse(Path file) throws IOException {
        return parse(file.getFileName().toString(), Files.readAllBytes(file));
    }

    public static ParseResult parse(String name, byte[] content) {
        if (isPdf(name, content)) {
            String pdfName = name.toLowerCase(Locale.ROOT).endsWith(".pdf") ? name : name + ".pdf";
            return new PdfReferenceExtractor().parse(pdfName, content);
        }
        return new BibParser().parse(name, content);
    }

    static boolean isPdf(String name, byte[] content) {
        boolean magic = content.length >= PDF_MAGIC.length
                && Arrays.equals(Arrays.copyOf(content, PDF_MAGIC.length), PDF_MAGIC);
        return magic || name.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }
}
