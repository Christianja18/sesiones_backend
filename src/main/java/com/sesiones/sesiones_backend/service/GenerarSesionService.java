package com.sesiones.sesiones_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.GenerateSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.EstandarAprendizaje;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.service.TemplateSessionGeneratorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenerarSesionService {

    private final ReferenceResolver referenceResolver;
    private final CapacidadRepository capacidadRepository;
    private final EstandarAprendizajeRepository estandarAprendizajeRepository;
    private final DesempenoRepository desempenoRepository;
    private final TemplateSessionGeneratorService templateSessionGeneratorService;

    @Transactional(readOnly = true)
    public SesionResponse execute(GenerateSesionRequest request) {
        referenceResolver.findNivel(request.getNivelId());
        Grado grado = referenceResolver.findGradoByNivel(request.getGradoId(), request.getNivelId());
        var area = referenceResolver.findArea(request.getAreaId());
        Competencia competencia = referenceResolver.findCompetenciaByArea(request.getCompetenciaId(), request.getAreaId());
        List<Capacidad> capacidades = capacidadRepository.findByCompetenciaIdOrderByIdAsc(competencia.getId());
        List<EstandarAprendizaje> estandares = grado.getCiclo() == null
            ? List.of()
            : estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(
                competencia.getId(),
                grado.getCiclo().getId()
            );
        List<Desempeno> desempenos = desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(grado.getId(), competencia.getId());

        return templateSessionGeneratorService.generate(request, grado, area, competencia, capacidades, estandares, desempenos);
    }
}

