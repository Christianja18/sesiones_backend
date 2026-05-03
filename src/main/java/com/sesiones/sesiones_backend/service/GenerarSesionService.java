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
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;

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
        List<Competencia> competencias = referenceResolver.findCompetenciasByArea(request.selectedCompetenciaIds(), request.getAreaId());
        List<Capacidad> capacidades = competencias.stream()
            .flatMap(competencia -> capacidadRepository.findByCompetenciaIdOrderByIdAsc(competencia.getId()).stream())
            .toList();
        String cicloId = requireCicloId(grado);
        List<EstandarAprendizaje> estandares = competencias.stream()
            .flatMap(competencia -> estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(
                competencia.getId(),
                cicloId
            ).stream())
            .toList();
        List<Desempeno> desempenos = competencias.stream()
            .flatMap(competencia -> desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(
                grado.getId(),
                competencia.getId()
            ).stream())
            .toList();

        return templateSessionGeneratorService.generate(request, grado, area, competencias, capacidades, estandares, desempenos);
    }

    private String requireCicloId(Grado grado) {
        if (grado.getCiclo() == null || grado.getCiclo().getId() == null || grado.getCiclo().getId().isBlank()) {
            throw new BusinessRuleException("El grado seleccionado no tiene ciclo educativo configurado");
        }
        return grado.getCiclo().getId();
    }
}

