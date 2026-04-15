package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.UpdateDocumentoCurriculoProcesamientoRequest;
import com.sesiones.sesiones_backend.util.enums.DocumentoProcesamientoEstado;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculoProcesamiento;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoProcesamientoRepository;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActualizarProcesamientoDocumentoCurriculoService {

    private final ReferenceResolver referenceResolver;
    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final DocumentoCurriculoProcesamientoRepository documentoCurriculoProcesamientoRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional
    public DocumentoCurriculoResponse execute(Integer documentoCurriculoId, UpdateDocumentoCurriculoProcesamientoRequest request) {
        DocumentoCurriculo documentoCurriculo = referenceResolver.findDocumentoCurriculo(documentoCurriculoId);
        DocumentoCurriculoProcesamiento procesamiento = documentoCurriculoProcesamientoRepository
            .findByDocumentoCurriculoId(documentoCurriculoId)
            .orElseGet(() -> {
                DocumentoCurriculoProcesamiento nuevoProcesamiento = new DocumentoCurriculoProcesamiento();
                nuevoProcesamiento.setDocumentoCurriculo(documentoCurriculo);
                return nuevoProcesamiento;
            });

        try {
            procesamiento.setEstado(DocumentoProcesamientoEstado.fromDatabaseValue(request.getEstado()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("El estado del procesamiento debe ser pendiente, en_proceso, procesado o error");
        }
        procesamiento.setObservacion(request.getObservacion() == null ? null : request.getObservacion().trim());
        procesamiento.setFechaUltimoProceso(request.getFechaUltimoProceso());

        DocumentoCurriculoProcesamiento savedProcessing = documentoCurriculoProcesamientoRepository.save(procesamiento);
        documentoCurriculo.setProcesamiento(savedProcessing);

        DocumentoCurriculo savedDocument = documentoCurriculoRepository.findDetailedById(documentoCurriculoId).orElse(documentoCurriculo);
        return sessionResponseMapper.toDocumentoCurriculoResponse(savedDocument);
    }
}


