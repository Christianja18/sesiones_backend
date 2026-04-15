package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObtenerDocumentoCurriculoService {

    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional(readOnly = true)
    public DocumentoCurriculoResponse execute(Long documentoCurriculoId) {
        return documentoCurriculoRepository.findDetailedById(documentoCurriculoId)
            .map(sessionResponseMapper::toDocumentoCurriculoResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el documento curricular con id " + documentoCurriculoId));
    }
}

