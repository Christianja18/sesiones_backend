package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.Institucion;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.InstitucionRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;
import com.sesiones.sesiones_backend.repository.UnidadRepository;
import com.sesiones.sesiones_backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReferenceResolver {

    private final NivelEducativoRepository nivelEducativoRepository;
    private final GradoRepository gradoRepository;
    private final AreaRepository areaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final CapacidadRepository capacidadRepository;
    private final DocenteRepository docenteRepository;
    private final InstitucionRepository institucionRepository;
    private final DocumentoCurriculoRepository documentoCurriculoRepository;
    private final UnidadRepository unidadRepository;

    public NivelEducativo findNivel(Long nivelId) {
        return nivelEducativoRepository.findById(nivelId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el nivel educativo con id " + nivelId));
    }

    public Grado findGrado(Long gradoId) {
        return gradoRepository.findById(gradoId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el grado con id " + gradoId));
    }

    public Grado findGradoByNivel(Long gradoId, Long nivelId) {
        return gradoRepository.findByIdAndNivelId(gradoId, nivelId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe el grado con id " + gradoId + " para el nivel educativo " + nivelId
            ));
    }

    public Area findArea(Long areaId) {
        return areaRepository.findById(areaId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el Ã¡rea con id " + areaId));
    }

    public Competencia findCompetenciaByArea(Long competenciaId, Long areaId) {
        return competenciaRepository.findByIdAndAreaId(competenciaId, areaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe la competencia con id " + competenciaId + " para el Ã¡rea " + areaId
            ));
    }

    public Docente findDocente(Long docenteId) {
        return docenteRepository.findById(docenteId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el docente con id " + docenteId));
    }

    public Institucion findInstitucion(Long institucionId) {
        return institucionRepository.findById(institucionId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la instituciÃ³n con id " + institucionId));
    }

    public Unidad findUnidad(Long unidadId) {
        return unidadRepository.findById(unidadId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la unidad con id " + unidadId));
    }

    public DocumentoCurriculo findDocumentoCurriculo(Long documentoCurriculoId) {
        return documentoCurriculoRepository.findById(documentoCurriculoId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el documento curricular con id " + documentoCurriculoId));
    }

    public List<Competencia> findCompetencias(List<Long> ids) {
        return validateCollection(ids, competenciaRepository::findByIdIn, "competencia");
    }

    public List<Capacidad> findCapacidades(List<Long> ids) {
        return validateCollection(ids, capacidadRepository::findByIdIn, "capacidad");
    }

    private <T> List<T> validateCollection(List<Long> ids, Function<List<Long>, List<T>> resolver, String label) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(ids);
        List<Long> normalizedIds = new ArrayList<>(uniqueIds);
        List<T> entities = resolver.apply(normalizedIds);
        if (entities.size() != normalizedIds.size()) {
            throw new ResourceNotFoundException("Uno o mÃ¡s registros de " + label + " no existen");
        }
        return entities;
    }
}

