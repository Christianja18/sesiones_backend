package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CreateUnidadRequest;
import com.sesiones.sesiones_backend.dto.UnidadResponse;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.repository.UnidadRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrearUnidadService {

    private final ReferenceResolver referenceResolver;
    private final UnidadRepository unidadRepository;
    private final SessionResponseMapper sessionResponseMapper;

    @Transactional
    public UnidadResponse execute(CreateUnidadRequest request) {
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new BusinessRuleException("La fecha fin no puede ser menor que la fecha inicio");
        }

        Unidad unidad = new Unidad();
        unidad.setTitulo(request.getTitulo().trim());
        unidad.setGrado(referenceResolver.findGrado(request.getGradoId()));
        unidad.setArea(referenceResolver.findArea(request.getAreaId()));
        unidad.setDocente(referenceResolver.findDocente(request.getDocenteId()));
        unidad.setFechaInicio(request.getFechaInicio());
        unidad.setFechaFin(request.getFechaFin());
        unidad.setContexto(request.getContexto().trim());

        Unidad saved = unidadRepository.save(unidad);
        return sessionResponseMapper.toUnidadResponse(saved);
    }
}

