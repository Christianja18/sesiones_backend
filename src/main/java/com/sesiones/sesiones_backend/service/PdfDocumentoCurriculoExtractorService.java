package com.sesiones.sesiones_backend.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.exception.BusinessRuleException;

@Service
public class PdfDocumentoCurriculoExtractorService {

    public String extractText(byte[] archivoPdf) {
        if (archivoPdf == null || archivoPdf.length == 0) {
            throw new BusinessRuleException("El PDF del documento curricular no contiene datos");
        }

        try (PDDocument document = Loader.loadPDF(archivoPdf)) {
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                throw new BusinessRuleException("No fue posible extraer texto util del PDF");
            }
            return text;
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible procesar el archivo PDF del documento curricular");
        }
    }
}
