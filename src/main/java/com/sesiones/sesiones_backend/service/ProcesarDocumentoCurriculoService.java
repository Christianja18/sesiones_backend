package com.sesiones.sesiones_backend.service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;
import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcesarDocumentoCurriculoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesarDocumentoCurriculoService.class);

    private final ReferenceResolver referenceResolver;
    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final DocumentoCurriculoDownloaderService documentoCurriculoDownloaderService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final DocumentoCurriculoChunkerService documentoCurriculoChunkerService;
    private final CurriculoLlmClient curriculoLlmClient;
    private final PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;
    private final SessionResponseMapper sessionResponseMapper;

    public DocumentoCurriculoResponse execute(Integer documentoCurriculoId) {
        DocumentoCurriculo documento = referenceResolver.findDocumentoCurriculo(documentoCurriculoId);
        markAsProcessing(documento);

        try {
            byte[] archivoPdf = documentoCurriculoDownloaderService.download(documento.getArchivoUrl());
            String checksumSha256 = sha256(archivoPdf);

            if (documentoCurriculoRepository.existsByChecksumSha256AndIdNot(checksumSha256, documentoCurriculoId)) {
                throw new BusinessRuleException("Ya existe otro documento curricular con el mismo contenido");
            }

            List<PaginaPdfTexto> pages = pdfTextExtractorService.extractPages(archivoPdf);
            List<DocumentoChunkContenido> chunks = documentoCurriculoChunkerService.chunk(pages);
            List<DocumentoChunkAnalizado> analisisChunks = analyzeChunks(chunks, documento.getTipo());

            poblarCurriculoDesdeDocumentoService.execute(documentoCurriculoId, checksumSha256, analisisChunks);
            DocumentoCurriculo actualizado = referenceResolver.findDocumentoCurriculo(documentoCurriculoId);
            return sessionResponseMapper.toDocumentoCurriculoResponse(actualizado);
        } catch (RuntimeException exception) {
            markAsError(documentoCurriculoId, exception.getMessage());
            throw exception;
        }
    }

    private List<DocumentoChunkAnalizado> analyzeChunks(List<DocumentoChunkContenido> chunks, DocumentoCurriculoTipo tipoDocumento) {
        if (tipoDocumento == null) {
            throw new BusinessRuleException("El tipo del documento curricular es obligatorio para procesar la ingesta");
        }

        List<DocumentoChunkAnalizado> analisis = new ArrayList<>();

        for (DocumentoChunkContenido chunk : chunks) {
            LOGGER.info(
                "Procesando chunk curricular. tipo={}, orden={}, paginas={}..{}, hash={}, contenidoPreview={}",
                tipoDocumento.getDatabaseValue(),
                chunk.orden(),
                chunk.paginaInicio(),
                chunk.paginaFin(),
                chunk.hashContenido(),
                abbreviate(chunk.contenido())
            );
            try {
                CurriculoDocumentoParseResponse response = curriculoLlmClient.extraerCurriculo(chunk.contenido(), tipoDocumento);
                int itemsCount = response == null || response.getItems() == null ? 0 : response.getItems().size();
                LOGGER.info(
                    "Chunk curricular procesado correctamente. tipo={}, orden={}, itemsExtraidos={}",
                    tipoDocumento.getDatabaseValue(),
                    chunk.orden(),
                    itemsCount
                );
                analisis.add(new DocumentoChunkAnalizado(chunk, response));
            } catch (RuntimeException exception) {
                LOGGER.error(
                    "Fallo la interpretacion de un chunk curricular. tipo={}, orden={}, paginas={}..{}, hash={}",
                    tipoDocumento.getDatabaseValue(),
                    chunk.orden(),
                    chunk.paginaInicio(),
                    chunk.paginaFin(),
                    chunk.hashContenido(),
                    exception
                );
                throw exception;
            }
        }

        return analisis;
    }

    private void markAsProcessing(DocumentoCurriculo documento) {
        documento.setEstado(ProcesamientoEstado.PROCESANDO);
        documento.setErrorDetalle(null);
        documento.setFechaProcesado(null);
        documentoCurriculoRepository.save(documento);
    }

    private void markAsError(Integer documentoCurriculoId, String errorDetalle) {
        DocumentoCurriculo documento = referenceResolver.findDocumentoCurriculo(documentoCurriculoId);
        documento.setEstado(ProcesamientoEstado.ERROR);
        documento.setErrorDetalle(normalizeError(errorDetalle));
        documento.setFechaProcesado(LocalDateTime.now());
        documentoCurriculoRepository.save(documento);
    }

    private String normalizeError(String errorDetalle) {
        if (errorDetalle == null || errorDetalle.isBlank()) {
            return "No fue posible procesar el documento curricular";
        }
        return errorDetalle.trim();
    }

    private String sha256(byte[] contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido);
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new BusinessRuleException("No fue posible calcular la huella del documento curricular");
        }
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }
}
