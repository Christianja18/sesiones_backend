package com.sesiones.sesiones_backend.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.exception.BusinessRuleException;

@Service
public class PdfTextExtractorService {

    public String extractText(byte[] archivoPdf) {
        if (archivoPdf == null || archivoPdf.length == 0) {
            throw new BusinessRuleException("El contenido del archivo PDF es obligatorio");
        }

        try (PDDocument documento = Loader.loadPDF(archivoPdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(documento);
            if (texto == null || texto.trim().isEmpty()) {
                throw new BusinessRuleException("No fue posible extraer texto util del PDF curricular");
            }
            return texto.trim();
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible leer el PDF curricular");
        }
    }
}
