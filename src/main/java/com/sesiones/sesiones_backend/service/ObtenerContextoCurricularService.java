package com.sesiones.sesiones_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CurriculumContextResponse;
import com.sesiones.sesiones_backend.dto.TextoReferenciaResponse;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObtenerContextoCurricularService {

    private final ReferenceResolver referenceResolver;
    private final CapacidadRepository capacidadRepository;
    private final DesempenoRepository desempenoRepository;

    @Transactional(readOnly = true)
    public CurriculumContextResponse execute(Integer nivelId, Integer gradoId, Integer areaId, Integer competenciaId) {
        var nivel = referenceResolver.findNivel(nivelId);
        var grado = referenceResolver.findGradoByNivel(gradoId, nivelId);
        var area = referenceResolver.findArea(areaId);
        var competencia = referenceResolver.findCompetenciaByArea(competenciaId, areaId);

        return CurriculumContextResponse.builder()
            .nivelId(nivel.getId())
            .nivelNombre(nivel.getNombre())
            .cicloId(grado.getCiclo() == null ? null : grado.getCiclo().getId())
            .cicloNombre(grado.getCiclo() == null ? null : grado.getCiclo().getNombre())
            .gradoId(grado.getId())
            .gradoNombre(grado.getNombre())
            .areaId(area.getId())
            .areaNombre(area.getNombre())
            .competenciaId(competencia.getId())
            .competenciaDescripcion(competencia.getDescripcion())
            .capacidades(capacidadRepository.findByCompetenciaIdOrderByIdAsc(competencia.getId()).stream()
                .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
                .toList())
            .desempenos(desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(grado.getId(), competencia.getId()).stream()
                .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
                .toList())
            .build();
    }
}


