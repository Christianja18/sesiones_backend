package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CreateDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.util.enums.DocumentoProcesamientoEstado;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculoProcesamiento;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoProcesamientoRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarDocumentoCurriculoService {

    private final ReferenceResolver referenceResolver;
    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final DocumentoCurriculoProcesamientoRepository documentoCurriculoProcesamientoRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional
    public DocumentoCurriculoResponse execute(CreateDocumentoCurriculoRequest request) {
        DocumentoCurriculo documentoCurriculo = new DocumentoCurriculo();
        documentoCurriculo.setNombreArchivo(request.getNombreArchivo().trim());
        documentoCurriculo.setRutaArchivo(request.getRutaArchivo().trim());
        documentoCurriculo.setArea(request.getAreaId() == null ? null : referenceResolver.findArea(request.getAreaId()));
        documentoCurriculo.setGrado(request.getGradoId() == null ? null : referenceResolver.findGrado(request.getGradoId()));

        DocumentoCurriculo savedDocument = documentoCurriculoRepository.save(documentoCurriculo);

        DocumentoCurriculoProcesamiento procesamiento = new DocumentoCurriculoProcesamiento();
        procesamiento.setDocumentoCurriculo(savedDocument);
        procesamiento.setEstado(DocumentoProcesamientoEstado.PENDIENTE);
        procesamiento.setObservacion("Pendiente de extracciÃƒÂ³n del contenido curricular");
        DocumentoCurriculoProcesamiento savedProcessing = documentoCurriculoProcesamientoRepository.save(procesamiento);
        savedDocument.setProcesamiento(savedProcessing);

        return sessionResponseMapper.toDocumentoCurriculoResponse(savedDocument);
    }
}

