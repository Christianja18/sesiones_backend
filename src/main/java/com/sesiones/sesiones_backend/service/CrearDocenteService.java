package com.sesiones.sesiones_backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CreateDocenteRequest;
import com.sesiones.sesiones_backend.dto.DocenteResponse;
import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.entity.Institucion;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.repository.InstitucionRepository;

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

        Docente docente = new Docente();
        docente.setNombre(request.getNombre().trim());
        docente.setEmail(normalizedEmail);
        resolveInstitution(request.getInstitucion()).ifPresent(docente::setInstitucion);

        Docente saved = docenteRepository.save(docente);
        Institucion institucion = saved.getInstitucion();
        return DocenteResponse.builder()
            .id(saved.getId())
            .nombre(saved.getNombre())
            .email(saved.getEmail())
            .institucionId(institucion == null ? null : institucion.getId())
            .institucion(institucion == null ? null : institucion.getNombre())
            .build();
    }

    private Optional<Institucion> resolveInstitution(String institutionName) {
        if (institutionName == null || institutionName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = institutionName.trim();
        return Optional.of(institucionRepository.findByNombreIgnoreCase(normalizedName)
            .orElseGet(() -> createInstitution(normalizedName)));
    }

    private Institucion createInstitution(String institutionName) {
        Institucion institucion = new Institucion();
        institucion.setNombre(institutionName);
        return institucionRepository.save(institucion);
    }
}

