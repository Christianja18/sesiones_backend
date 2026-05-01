package com.sesiones.sesiones_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.exception.BusinessRuleException;

@Service
public class DocumentoCurriculoChunkerService {

    private final int maxCharsPerChunk;
    private final int maxParagraphsPerChunk;

    public DocumentoCurriculoChunkerService(
        @Value("${app.curriculo.chunk.max-chars:3500}") int maxCharsPerChunk,
        @Value("${app.curriculo.chunk.max-paragraphs:6}") int maxParagraphsPerChunk
    ) {
        this.maxCharsPerChunk = maxCharsPerChunk;
        this.maxParagraphsPerChunk = maxParagraphsPerChunk;
    }

    public List<DocumentoChunkContenido> chunk(List<PaginaPdfTexto> pages) {
        if (pages == null || pages.isEmpty()) {
            throw new BusinessRuleException("No existe texto suficiente para generar chunks curriculares");
        }

        List<DocumentoChunkContenido> chunks = new ArrayList<>();
        List<String> currentParagraphs = new ArrayList<>();
        Integer currentPageStart = null;
        Integer currentPageEnd = null;
        int currentChars = 0;
        int order = 1;

        for (PaginaPdfTexto page : pages) {
            for (String paragraph : splitParagraphs(page.contenido())) {
                if (currentParagraphs.isEmpty()) {
                    currentPageStart = page.numeroPagina();
                }

                boolean chunkLimitReached = currentChars > 0
                    && (currentChars + paragraph.length() > maxCharsPerChunk
                        || currentParagraphs.size() >= maxParagraphsPerChunk);

                if (chunkLimitReached) {
                    chunks.add(buildChunk(order++, currentPageStart, currentPageEnd, currentParagraphs));
                    currentParagraphs = new ArrayList<>();
                    currentChars = 0;
                    currentPageStart = page.numeroPagina();
                }

                currentParagraphs.add(paragraph);
                currentChars += paragraph.length();
                currentPageEnd = page.numeroPagina();
            }
        }

        if (!currentParagraphs.isEmpty()) {
            chunks.add(buildChunk(order, currentPageStart, currentPageEnd, currentParagraphs));
        }

        if (chunks.isEmpty()) {
            throw new BusinessRuleException("No fue posible construir chunks curriculares a partir del documento");
        }

        return chunks;
    }

    private DocumentoChunkContenido buildChunk(
        int order,
        Integer pageStart,
        Integer pageEnd,
        List<String> paragraphs
    ) {
        String contenido = String.join("\n\n", paragraphs).trim();
        if (contenido.isEmpty()) {
            throw new BusinessRuleException("Se genero un chunk curricular vacio");
        }
        return new DocumentoChunkContenido(order, pageStart, pageEnd, contenido, sha256(contenido));
    }

    private List<String> splitParagraphs(String contenidoPagina) {
        String normalized = contenidoPagina == null
            ? ""
            : Normalizer.normalize(contenidoPagina, Normalizer.Form.NFKC).trim();

        if (normalized.isEmpty()) {
            return List.of();
        }

        String[] rawParts = normalized.split("(\\r?\\n){2,}");
        List<String> paragraphs = new ArrayList<>();
        for (String rawPart : rawParts) {
            String paragraph = rawPart.replaceAll("\\s+", " ").trim();
            if (!paragraph.isEmpty()) {
                paragraphs.add(paragraph);
            }
        }

        if (paragraphs.isEmpty()) {
            return List.of(normalized.replaceAll("\\s+", " ").trim());
        }
        return paragraphs;
    }

    private String sha256(String contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessRuleException("No fue posible generar el hash del chunk curricular");
        }
    }
}
