package com.sesiones.sesiones_backend.service;

import java.io.IOException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.UploadDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarDocumentoCurriculoService {

    private final ReferenceResolver referenceResolver;
    private final PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;
    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional
    public DocumentoCurriculoResponse execute(UploadDocumentoCurriculoRequest request) {
        MultipartFile archivo = request.getArchivo();
        validatePdfFile(archivo);
        byte[] archivoPdf = readPdfContent(archivo);

        Area areaSugerida = request.getAreaId() == null ? null : referenceResolver.findArea(request.getAreaId());
        Grado gradoSugerido = request.getGradoId() == null ? null : referenceResolver.findGrado(request.getGradoId());
        ResultadoPoblacionCurricular resultadoPoblacion = poblarCurriculoDesdeDocumentoService.execute(
            archivoPdf,
            areaSugerida,
            gradoSugerido
        );

        DocumentoCurriculo documentoCurriculo = new DocumentoCurriculo();
        documentoCurriculo.setNombreArchivo(normalizeFileName(archivo));
        documentoCurriculo.setArchivoPdf(archivoPdf);
        documentoCurriculo.setArea(resultadoPoblacion.getArea());
        documentoCurriculo.setGrado(resultadoPoblacion.getGrado());

        DocumentoCurriculo savedDocument = documentoCurriculoRepository.save(documentoCurriculo);
        return sessionResponseMapper.toDocumentoCurriculoResponse(savedDocument);
    }

    private void validatePdfFile(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("El archivo PDF es obligatorio");
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            throw new BusinessRuleException("No fue posible identificar el nombre del archivo PDF");
        }

        String normalizedName = nombreArchivo.trim().toLowerCase(Locale.ROOT);
        if (!normalizedName.endsWith(".pdf")) {
            throw new BusinessRuleException("Solo se permite registrar archivos con extension .pdf");
        }
    }

    private String normalizeFileName(MultipartFile archivo) {
        return archivo.getOriginalFilename().trim();
    }

    private byte[] readPdfContent(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible leer el contenido del archivo PDF");
        }
    }
}
