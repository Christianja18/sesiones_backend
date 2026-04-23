package com.sesiones.sesiones_backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.exception.BusinessRuleException;

@Service
public class PdfTextExtractorService {

    public String extractText(byte[] archivoPdf) {
        return extractPages(archivoPdf).stream()
            .map(PaginaPdfTexto::contenido)
            .reduce((left, right) -> left + "\n\n" + right)
            .orElseThrow(() -> new BusinessRuleException("No fue posible extraer texto util del PDF curricular"));
    }

    public List<PaginaPdfTexto> extractPages(byte[] archivoPdf) {
        if (archivoPdf == null || archivoPdf.length == 0) {
            throw new BusinessRuleException("El contenido del archivo PDF es obligatorio");
        }

        try (PDDocument documento = Loader.loadPDF(archivoPdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PaginaPdfTexto> pages = new ArrayList<>();
            int totalPaginas = documento.getNumberOfPages();

            for (int numeroPagina = 1; numeroPagina <= totalPaginas; numeroPagina++) {
                stripper.setStartPage(numeroPagina);
                stripper.setEndPage(numeroPagina);
                String texto = stripper.getText(documento);
                if (texto == null) {
                    continue;
                }

                String contenido = texto.trim();
                if (!contenido.isEmpty()) {
                    pages.add(new PaginaPdfTexto(numeroPagina, contenido));
                }
            }

            if (pages.isEmpty()) {
                throw new BusinessRuleException("No fue posible extraer texto util del PDF curricular");
            }

            return pages;
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible leer el PDF curricular");
        }
    }
}
