package com.sesiones.sesiones_backend.service;

import java.io.IOException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.UploadDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarDocumentoCurriculoService {

    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional
    public DocumentoCurriculoResponse execute(UploadDocumentoCurriculoRequest request) {
        MultipartFile archivo = request.getArchivo();
        validatePdf(archivo);
        byte[] archivoPdf = readPdfBytes(archivo);

        PoblarCurriculoDesdeDocumentoService.ResultadoPoblacion resultado =
            poblarCurriculoDesdeDocumentoService.execute(archivoPdf, request.getAreaId(), request.getGradoId());

        DocumentoCurriculo documentoCurriculo = new DocumentoCurriculo();
        documentoCurriculo.setNombreArchivo(resolveFileName(archivo));
        documentoCurriculo.setArchivoPdf(archivoPdf);
        documentoCurriculo.setArea(resultado.getArea());
        documentoCurriculo.setGrado(resultado.getGrado());

        DocumentoCurriculo savedDocument = documentoCurriculoRepository.save(documentoCurriculo);
        return sessionResponseMapper.toDocumentoCurriculoResponse(savedDocument);
    }

    private void validatePdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("Debes adjuntar un archivo PDF");
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || !nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessRuleException("El archivo debe tener extension .pdf");
        }

        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BusinessRuleException("El archivo debe ser un PDF valido");
        }
    }

    private byte[] readPdfBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible leer el archivo PDF enviado");
        }
    }

    private String resolveFileName(MultipartFile archivo) {
        String nombreArchivo = archivo.getOriginalFilename();
        return nombreArchivo == null || nombreArchivo.isBlank() ? "documento_curricular.pdf" : nombreArchivo.trim();
    }
}
