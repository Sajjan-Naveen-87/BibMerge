package io.bibmerge.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** One-page PDFs built in memory, so tests need no binary fixtures. */
public final class TestPdfs {

    private TestPdfs() {
    }

    public static byte[] pdf(List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream text = new PDPageContentStream(doc, page)) {
                text.beginText();
                text.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                text.setLeading(11);
                text.newLineAtOffset(40, 750);
                for (String line : lines) {
                    text.showText(line);
                    text.newLine();
                }
                text.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
