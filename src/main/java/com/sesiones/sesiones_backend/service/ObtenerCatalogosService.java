package com.sesiones.sesiones_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.CatalogOptionResponse;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CicloRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;
import com.sesiones.sesiones_backend.repository.SesionRepository;
import com.sesiones.sesiones_backend.repository.UnidadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObtenerCatalogosService {

    private final NivelEducativoRepository nivelEducativoRepository;
    private final CicloRepository cicloRepository;
    private final GradoRepository gradoRepository;
    private final AreaRepository areaRepository;
    private final CompetenciaRepository competenciaRepository;
    private final CapacidadRepository capacidadRepository;
    private final EstandarAprendizajeRepository estandarAprendizajeRepository;
    private final DesempenoRepository desempenoRepository;
    private final DocenteRepository docenteRepository;
    private final UnidadRepository unidadRepository;
    private final SesionRepository sesionRepository;

    public List<CatalogOptionResponse> niveles() {
        return nivelEducativoRepository.findAllByOrderByNombreAsc().stream()
            .map(nivel -> option(nivel.getId(), nivel.getNombre()))
            .toList();
    }

    public List<CatalogOptionResponse> ciclos() {
        return cicloRepository.findAllByOrderByIdAsc().stream()
            .map(ciclo -> option(ciclo.getId(), ciclo.getNombre()))
            .toList();
    }

    public List<CatalogOptionResponse> grados(Integer nivelId) {
        return gradoRepository.findCatalog(nivelId).stream()
            .map(grado -> option(grado.getId(), grado.getNombre()))
            .toList();
    }

    public List<CatalogOptionResponse> areas() {
        return areaRepository.findAllByOrderByNombreAsc().stream()
            .map(area -> option(area.getId(), area.getNombre()))
            .toList();
    }

    public List<CatalogOptionResponse> competencias(Integer areaId) {
        return competenciaRepository.findCatalog(areaId).stream()
            .map(competencia -> option(competencia.getId(), competencia.getDescripcion()))
            .toList();
    }

    public List<CatalogOptionResponse> capacidades(Integer competenciaId) {
        return capacidadRepository.findCatalog(competenciaId).stream()
            .map(capacidad -> option(capacidad.getId(), capacidad.getDescripcion()))
            .toList();
    }

    public List<CatalogOptionResponse> estandares(Integer competenciaId, String cicloId) {
        return estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(competenciaId, cicloId).stream()
            .map(estandar -> option(estandar.getId(), estandar.getDescripcion()))
            .toList();
    }

    public List<CatalogOptionResponse> desempenos(Integer gradoId, Integer competenciaId) {
        return desempenoRepository.findCatalog(gradoId, competenciaId).stream()
            .map(desempeno -> option(desempeno.getId(), desempeno.getDescripcion()))
            .toList();
    }

    public List<CatalogOptionResponse> docentes() {
        return docenteRepository.findAllByOrderByNombreAsc().stream()
            .map(docente -> option(docente.getId(), docente.getNombre()))
            .toList();
    }

    public List<CatalogOptionResponse> unidades(Integer docenteId) {
        return unidadRepository.findByDocenteIdOrderByFechaInicioDescIdDesc(docenteId).stream()
            .map(unidad -> option(unidad.getId(), unidad.getTitulo()))
            .toList();
    }

    public List<CatalogOptionResponse> sesiones(Integer unidadId) {
        return sesionRepository.findByUnidadIdOrderByFechaDescIdDesc(unidadId).stream()
            .map(sesion -> option(sesion.getId(), sesion.getTitulo()))
            .toList();
    }

    private CatalogOptionResponse option(Object id, String nombre) {
        return CatalogOptionResponse.builder()
            .id(String.valueOf(id))
            .nombre(nombre)
            .build();
    }
}
