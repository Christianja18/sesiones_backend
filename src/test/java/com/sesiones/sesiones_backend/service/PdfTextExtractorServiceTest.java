package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import com.sesiones.sesiones_backend.exception.BusinessRuleException;

class PdfTextExtractorServiceTest {

    private final PdfTextExtractorService pdfTextExtractorService = new PdfTextExtractorService();

    @Test
    void shouldRejectDownloadedContentThatIsNotPdf() {
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> pdfTextExtractorService.extractPages("<html>login</html>".getBytes())
        );

        assertTrue(exception.getMessage().contains("no es un PDF valido"));
    }

    @Test
    void shouldExplainWhenPdfHasNoExtractableText() throws IOException {
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> pdfTextExtractorService.extractPages(buildEmptyPdf())
        );

        assertTrue(exception.getMessage().contains("no contiene capa de texto extraible"));
        assertTrue(exception.getMessage().contains("PDF con texto seleccionable"));
    }

    @Test
    void shouldExtractTextFromPdfWithTextLayer() throws IOException {
        List<PaginaPdfTexto> pages = pdfTextExtractorService.extractPages(buildTextPdf("Competencia curricular"));

        assertEquals(1, pages.size());
        assertEquals(1, pages.get(0).numeroPagina());
        assertTrue(pages.get(0).contenido().contains("Competencia curricular"));
    }

    private byte[] buildEmptyPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] buildTextPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
