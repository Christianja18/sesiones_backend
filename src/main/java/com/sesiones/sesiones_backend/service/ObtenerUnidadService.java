package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.UnidadResponse;
import com.sesiones.sesiones_backend.repository.UnidadRepository;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObtenerUnidadService {

    private final UnidadRepository unidadRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional(readOnly = true)
    public UnidadResponse execute(Integer unidadId) {
        return unidadRepository.findDetailedById(unidadId)
            .map(sessionResponseMapper::toUnidadResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la unidad con id " + unidadId));
    }
}


