package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.SaveSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.CriterioEvaluacion;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Evidencia;
import com.sesiones.sesiones_backend.entity.InstrumentoEvaluacion;
import com.sesiones.sesiones_backend.entity.Sesion;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.SesionRepository;
import com.sesiones.sesiones_backend.service.RecursiveActivityAssembler;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuardarSesionService {

    private final ReferenceResolver referenceResolver;
    private final DesempenoRepository desempenoRepository;
    private final RecursiveActivityAssembler recursiveActivityAssembler;
    private final SessionResponseMapper sessionResponseMapper;
    private final SesionRepository sesionRepository;

    @Transactional
    public SesionResponse execute(SaveSesionRequest request) {
        if (request.getCompetenciaIds() == null || request.getCompetenciaIds().isEmpty()) {
            throw new BusinessRuleException("La sesiÃƒÂ³n debe incluir al menos una competencia");
        }

        Unidad unidad = referenceResolver.findUnidad(request.getUnidadId());
        Sesion sesion = new Sesion();
        sesion.setUnidad(unidad);
        sesion.setFecha(request.getFecha());
        sesion.setTitulo(request.getTitulo().trim());
        sesion.setProposito(request.getProposito().trim());
        sesion.setDuracionMinutos(request.getDuracionMinutos());
        sesion.setGeneradoPorIa(request.isGeneradoPorIa());

        sesion.setCompetencias(validateCompetencias(
            new ArrayList<>(referenceResolver.findCompetencias(uniqueIds(request.getCompetenciaIds()))),
            unidad
        ));
        sesion.setCapacidades(validateCapacidades(referenceResolver.findCapacidades(uniqueIds(request.getCapacidadIds())), sesion.getCompetencias()));
        sesion.setDesempenos(validateDesempenos(uniqueIds(request.getDesempenoIds()), sesion.getCompetencias(), unidad));
        sesion.setActividades(recursiveActivityAssembler.assemble(sesion, request.getActividades()));
        if (sesion.getActividades().isEmpty()) {
            throw new BusinessRuleException("La sesiÃƒÂ³n debe incluir al menos una actividad");
        }
        sesion.setCriteriosEvaluacion(buildCriteria(request, sesion));
        sesion.setEvidencias(buildEvidence(request, sesion));
        sesion.setInstrumentosEvaluacion(List.of(buildInstrument(request, sesion)));

        Sesion saved = sesionRepository.save(sesion);
        return sessionResponseMapper.toSesionResponse(sesionRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    private List<Capacidad> validateCapacidades(List<Capacidad> capacidades, List<Competencia> competencias) {
        List<Integer> competenciaIds = competencias.stream().map(Competencia::getId).toList();
        boolean invalid = capacidades.stream().anyMatch(item -> !competenciaIds.contains(item.getCompetencia().getId()));
        if (invalid) {
            throw new BusinessRuleException("Todas las capacidades deben pertenecer a las competencias seleccionadas");
        }
        return capacidades;
    }

    private List<Competencia> validateCompetencias(List<Competencia> competencias, Unidad unidad) {
        boolean invalid = competencias.stream().anyMatch(item -> !item.getArea().getId().equals(unidad.getArea().getId()));
        if (invalid) {
            throw new BusinessRuleException("Todas las competencias deben corresponder al ÃƒÂ¡rea de la unidad");
        }
        return competencias;
    }

    private List<Desempeno> validateDesempenos(List<Integer> desempenoIds, List<Competencia> competencias, Unidad unidad) {
        if (desempenoIds.isEmpty()) {
            return List.of();
        }

        List<Desempeno> desempenos = desempenoRepository.findByIdIn(desempenoIds);
        if (desempenos.size() != desempenoIds.size()) {
            throw new BusinessRuleException("Uno o mÃƒÂ¡s desempeÃƒÂ±os no existen");
        }

        List<Integer> competenciaIds = competencias.stream().map(Competencia::getId).toList();
        boolean invalid = desempenos.stream().anyMatch(item ->
            !competenciaIds.contains(item.getCompetencia().getId())
                || !item.getGrado().getId().equals(unidad.getGrado().getId())
        );
        if (invalid) {
            throw new BusinessRuleException("Todos los desempeÃƒÂ±os deben corresponder al grado y competencias de la sesiÃƒÂ³n");
        }
        return desempenos;
    }

    private List<CriterioEvaluacion> buildCriteria(SaveSesionRequest request, Sesion sesion) {
        return cleanText(request.getCriteriosEvaluacion()).stream()
            .map(text -> {
                CriterioEvaluacion criterio = new CriterioEvaluacion();
                criterio.setSesion(sesion);
                criterio.setDescripcion(text);
                return criterio;
            })
            .toList();
    }

    private List<Evidencia> buildEvidence(SaveSesionRequest request, Sesion sesion) {
        return cleanText(request.getEvidencias()).stream()
            .map(text -> {
                Evidencia evidencia = new Evidencia();
                evidencia.setSesion(sesion);
                evidencia.setDescripcion(text);
                return evidencia;
            })
            .toList();
    }

    private InstrumentoEvaluacion buildInstrument(SaveSesionRequest request, Sesion sesion) {
        InstrumentoEvaluacion instrumento = new InstrumentoEvaluacion();
        instrumento.setSesion(sesion);
        try {
            instrumento.setTipo(InstrumentoTipo.valueOf(request.getInstrumentoEvaluacion().getTipo().trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("El tipo de instrumento debe ser RUBRICA o LISTA_COTEJO");
        }
        instrumento.setContenidoJson(sessionResponseMapper.toInstrumentJson(request.getInstrumentoEvaluacion()));
        return instrumento;
    }

    private List<Integer> uniqueIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private List<String> cleanText(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .toList();
    }
}


