package com.company.resumeai.parser;

import com.company.resumeai.common.exception.InvalidRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * §9 "Extract Text" step. Three formats: PDF (PDFBox), DOCX (POI), and plain
 * .txt (no library needed). A bad/corrupt file is treated as a client error
 * (400, InvalidRequestException) rather than a 500 - it's a property of the
 * specific upload, not a server/config problem, and the caller can just fix
 * or re-export the file and retry.
 */
@Component
public class ResumeTextExtractor {

    public String extract(String fileName, byte[] content) {
        String extension = extensionOf(fileName);
        return switch (extension) {
            case "pdf" -> extractPdf(content);
            case "docx" -> extractDocx(content);
            case "txt" -> new String(content, StandardCharsets.UTF_8);
            default -> throw new InvalidRequestException(
                    "Unsupported resume file type: ." + extension + " (supported: .pdf, .docx, .txt)");
        };
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new InvalidRequestException("Could not extract text from PDF - file may be corrupt or encrypted: "
                    + e.getMessage());
        }
    }

    private String extractDocx(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (IOException e) {
            throw new InvalidRequestException("Could not extract text from DOCX - file may be corrupt: "
                    + e.getMessage());
        }
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new InvalidRequestException("File has no extension: " + fileName);
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
