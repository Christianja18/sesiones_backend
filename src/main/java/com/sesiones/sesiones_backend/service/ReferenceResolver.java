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

    public NivelEducativo findNivel(Integer nivelId) {
        return nivelEducativoRepository.findById(nivelId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el nivel educativo con id " + nivelId));
    }

    public Grado findGrado(Integer gradoId) {
        return gradoRepository.findById(gradoId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el grado con id " + gradoId));
    }

    public Grado findGradoByNivel(Integer gradoId, Integer nivelId) {
        return gradoRepository.findByIdAndNivelId(gradoId, nivelId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe el grado con id " + gradoId + " para el nivel educativo " + nivelId
            ));
    }

    public Area findArea(Integer areaId) {
        return areaRepository.findById(areaId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el ÃƒÂ¡rea con id " + areaId));
    }

    public Competencia findCompetenciaByArea(Integer competenciaId, Integer areaId) {
        return competenciaRepository.findByIdAndAreaId(competenciaId, areaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe la competencia con id " + competenciaId + " para el ÃƒÂ¡rea " + areaId
            ));
    }

    public Docente findDocente(Integer docenteId) {
        return docenteRepository.findById(docenteId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el docente con id " + docenteId));
    }

    public Institucion findInstitucion(Integer institucionId) {
        return institucionRepository.findById(institucionId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la instituciÃƒÂ³n con id " + institucionId));
    }

    public Unidad findUnidad(Integer unidadId) {
        return unidadRepository.findById(unidadId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la unidad con id " + unidadId));
    }

    public DocumentoCurriculo findDocumentoCurriculo(Integer documentoCurriculoId) {
        return documentoCurriculoRepository.findById(documentoCurriculoId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el documento curricular con id " + documentoCurriculoId));
    }

    public List<Competencia> findCompetencias(List<Integer> ids) {
        return validateCollection(ids, competenciaRepository::findByIdIn, "competencia");
    }

    public List<Capacidad> findCapacidades(List<Integer> ids) {
        return validateCollection(ids, capacidadRepository::findByIdIn, "capacidad");
    }

    private <T> List<T> validateCollection(List<Integer> ids, Function<List<Integer>, List<T>> resolver, String label) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<Integer> uniqueIds = new LinkedHashSet<>(ids);
        List<Integer> normalizedIds = new ArrayList<>(uniqueIds);
        List<T> entities = resolver.apply(normalizedIds);
        if (entities.size() != normalizedIds.size()) {
            throw new ResourceNotFoundException("Uno o mÃƒÂ¡s registros de " + label + " no existen");
        }
        return entities;
    }
}


