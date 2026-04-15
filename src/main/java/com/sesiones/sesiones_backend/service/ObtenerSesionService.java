package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.repository.SesionRepository;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObtenerSesionService {

    private final SesionRepository sesionRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional(readOnly = true)
    public SesionResponse execute(Long sesionId) {
        return sesionRepository.findDetailedById(sesionId)
            .map(sessionResponseMapper::toSesionResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la sesiÃ³n con id " + sesionId));
    }
}

