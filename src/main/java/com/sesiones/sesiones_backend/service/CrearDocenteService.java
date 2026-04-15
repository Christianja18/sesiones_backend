package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CreateDocenteRequest;
import com.sesiones.sesiones_backend.dto.DocenteResponse;
import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.entity.Institucion;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.repository.InstitucionRepository;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrearDocenteService {

    private final DocenteRepository docenteRepository;
    private final InstitucionRepository institucionRepository;

    @Transactional
    public DocenteResponse execute(CreateDocenteRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (docenteRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessRuleException("Ya existe un docente registrado con el email " + normalizedEmail);
        }

        Institucion institucion = institucionRepository.findByNombreIgnoreCase(request.getInstitucion().trim())
            .orElseGet(() -> createInstitution(request.getInstitucion().trim()));

        Docente docente = new Docente();
        docente.setNombre(request.getNombre().trim());
        docente.setEmail(normalizedEmail);
        docente.setInstitucion(institucion);

        Docente saved = docenteRepository.save(docente);
        return DocenteResponse.builder()
            .id(saved.getId())
            .nombre(saved.getNombre())
            .email(saved.getEmail())
            .institucionId(saved.getInstitucion().getId())
            .institucion(saved.getInstitucion().getNombre())
            .build();
    }

    private Institucion createInstitution(String institutionName) {
        Institucion institucion = new Institucion();
        institucion.setNombre(institutionName);
        return institucionRepository.save(institucion);
    }
}

