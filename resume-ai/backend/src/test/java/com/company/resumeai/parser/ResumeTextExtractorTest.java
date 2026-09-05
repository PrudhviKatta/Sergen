package com.company.resumeai.parser;

import com.company.resumeai.common.exception.InvalidRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PDF/DOCX fixtures are built in-test (round-trip: write known text, then
 * extract it back) rather than committed binary files - no repo bloat, and
 * the test is self-documenting about exactly what content it expects back.
 */
class ResumeTextExtractorTest {

    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void extractsPlainText() {
        String text = extractor.extract("resume.txt",
                "Jane Doe, Java Developer".getBytes(StandardCharsets.UTF_8));

        assertThat(text).isEqualTo("Jane Doe, Java Developer");
    }

    @Test
    void extractsTextFromPdf() throws IOException {
        byte[] pdfBytes = buildSimplePdf("Jane Doe - Java Full Stack Developer");

        String text = extractor.extract("resume.pdf", pdfBytes);

        assertThat(text).contains("Jane Doe - Java Full Stack Developer");
    }

    @Test
    void extractsTextFromDocx() throws IOException {
        byte[] docxBytes = buildSimpleDocx("Jane Doe - Java Full Stack Developer");

        String text = extractor.extract("resume.docx", docxBytes);

        assertThat(text).contains("Jane Doe - Java Full Stack Developer");
    }

    @Test
    void rejectsUnsupportedFileType() {
        assertThatThrownBy(() -> extractor.extract("resume.rtf", new byte[]{1, 2, 3}))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported resume file type");
    }

    @Test
    void rejectsFileWithNoExtension() {
        assertThatThrownBy(() -> extractor.extract("resume", new byte[]{1, 2, 3}))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("no extension");
    }

    @Test
    void rejectsCorruptPdf() {
        assertThatThrownBy(() -> extractor.extract("resume.pdf", "not a real pdf".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidRequestException.class);
    }

    private byte[] buildSimplePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildSimpleDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
