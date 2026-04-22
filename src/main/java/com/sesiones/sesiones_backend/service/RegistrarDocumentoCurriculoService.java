package com.sesiones.sesiones_backend.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.RegisterDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarDocumentoCurriculoService {

    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional
    public DocumentoCurriculoResponse execute(RegisterDocumentoCurriculoRequest request) {
        validateRequest(request);

        String nombreArchivo = normalizeFileName(request.getNombreArchivo());
        String archivoUrl = normalizeArchivoUrl(request.getArchivoUrl());

        validatePdfFileName(nombreArchivo);
        validateArchivoUrl(archivoUrl);

        if (documentoCurriculoRepository.existsByArchivoUrl(archivoUrl)) {
            throw new BusinessRuleException("Ya existe un documento curricular registrado con la misma URL");
        }

        DocumentoCurriculo documentoCurriculo = new DocumentoCurriculo();
        documentoCurriculo.setNombreArchivo(nombreArchivo);
        documentoCurriculo.setArchivoUrl(archivoUrl);
        documentoCurriculo.setEstado(ProcesamientoEstado.PENDIENTE);

        DocumentoCurriculo savedDocument = documentoCurriculoRepository.save(documentoCurriculo);
        return sessionResponseMapper.toDocumentoCurriculoResponse(savedDocument);
    }

    private void validateRequest(RegisterDocumentoCurriculoRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La referencia del documento curricular es obligatoria");
        }
    }

    private void validatePdfFileName(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new BusinessRuleException("El nombre del archivo PDF es obligatorio");
        }

        String normalizedName = nombreArchivo.toLowerCase(Locale.ROOT);
        if (!normalizedName.endsWith(".pdf")) {
            throw new BusinessRuleException("El nombre del archivo debe tener extension .pdf");
        }
    }

    private void validateArchivoUrl(String archivoUrl) {
        if (archivoUrl == null || archivoUrl.isBlank()) {
            throw new BusinessRuleException("La URL del documento curricular es obligatoria");
        }

        try {
            URI uri = new URI(archivoUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new BusinessRuleException("La URL del documento debe usar http o https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new BusinessRuleException("La URL del documento curricular no es valida");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessRuleException("La URL del documento curricular no es valida");
        }
    }

    private String normalizeFileName(String nombreArchivo) {
        return nombreArchivo == null ? null : nombreArchivo.trim();
    }

    private String normalizeArchivoUrl(String archivoUrl) {
        return archivoUrl == null ? null : archivoUrl.trim();
    }
}
