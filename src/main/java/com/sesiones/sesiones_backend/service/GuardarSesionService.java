package com.sesiones.sesiones_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.dto.SaveSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.CriterioEvaluacion;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Evidencia;
import com.sesiones.sesiones_backend.entity.InstrumentoEvaluacion;
import com.sesiones.sesiones_backend.entity.Sesion;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.SesionRepository;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuardarSesionService {

    private final ReferenceResolver referenceResolver;
    private final DesempenoRepository desempenoRepository;
    private final ActivityAssembler activityAssembler;
    private final SessionResponseMapper sessionResponseMapper;
    private final SesionRepository sesionRepository;

    @Transactional
    public SesionResponse execute(SaveSesionRequest request) {
        validateRequest(request);
        if (request.getCompetenciaIds() == null || request.getCompetenciaIds().isEmpty()) {
            throw new BusinessRuleException("La sesion debe incluir al menos una competencia");
        }

        List<String> criterios = cleanText(request.getCriteriosEvaluacion());
        List<String> evidencias = cleanText(request.getEvidencias());
        List<String> detalleInstrumento = cleanText(request.getInstrumentoEvaluacion().getDetalle());
        validateRequiredSessionContent(request.getActividades(), criterios, evidencias, detalleInstrumento);

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
        sesion.setActividades(activityAssembler.assemble(sesion, request.getActividades()));
        sesion.setCriteriosEvaluacion(buildCriteria(criterios, sesion));
        sesion.setEvidencias(buildEvidence(evidencias, sesion));
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
            throw new BusinessRuleException("Todas las competencias deben corresponder al area de la unidad");
        }
        return competencias;
    }

    private List<Desempeno> validateDesempenos(List<Integer> desempenoIds, List<Competencia> competencias, Unidad unidad) {
        if (desempenoIds.isEmpty()) {
            return List.of();
        }

        List<Desempeno> desempenos = desempenoRepository.findByIdIn(desempenoIds);
        if (desempenos.size() != desempenoIds.size()) {
            throw new BusinessRuleException("Uno o mas desempenos no existen");
        }

        List<Integer> competenciaIds = competencias.stream().map(Competencia::getId).toList();
        boolean invalid = desempenos.stream().anyMatch(item ->
            !competenciaIds.contains(item.getCompetencia().getId())
                || !item.getGrado().getId().equals(unidad.getGrado().getId())
        );
        if (invalid) {
            throw new BusinessRuleException("Todos los desempenos deben corresponder al grado y competencias de la sesion");
        }
        return desempenos;
    }

    private List<CriterioEvaluacion> buildCriteria(List<String> criterios, Sesion sesion) {
        return criterios.stream()
            .map(text -> {
                CriterioEvaluacion criterio = new CriterioEvaluacion();
                criterio.setSesion(sesion);
                criterio.setDescripcion(text);
                return criterio;
            })
            .toList();
    }

    private List<Evidencia> buildEvidence(List<String> evidencias, Sesion sesion) {
        return evidencias.stream()
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
            instrumento.setTipo(InstrumentoTipo.fromDatabaseValue(request.getInstrumentoEvaluacion().getTipo()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("El tipo de instrumento debe ser rubrica o lista_cotejo");
        }
        instrumento.setContenidoJson(sessionResponseMapper.toInstrumentJson(request.getInstrumentoEvaluacion()));
        return instrumento;
    }

    private void validateRequest(SaveSesionRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La solicitud de sesion es obligatoria");
        }
        if (request.getActividades() == null) {
            throw new BusinessRuleException("La sesion debe incluir actividades de inicio, desarrollo y cierre");
        }
        if (request.getInstrumentoEvaluacion() == null) {
            throw new BusinessRuleException("La sesion debe incluir un instrumento de evaluacion");
        }
    }

    private void validateRequiredSessionContent(
        ActividadesSesionDto actividades,
        List<String> criterios,
        List<String> evidencias,
        List<String> detalleInstrumento
    ) {
        if (!hasText(actividades.getInicio()) || !hasText(actividades.getDesarrollo()) || !hasText(actividades.getCierre())) {
            throw new BusinessRuleException("La sesion debe incluir actividades de inicio, desarrollo y cierre");
        }
        if (criterios.isEmpty()) {
            throw new BusinessRuleException("La sesion debe incluir al menos un criterio de evaluacion");
        }
        if (evidencias.isEmpty()) {
            throw new BusinessRuleException("La sesion debe incluir al menos una evidencia");
        }
        if (detalleInstrumento.isEmpty()) {
            throw new BusinessRuleException("El instrumento de evaluacion debe incluir al menos un detalle");
        }
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

    private boolean hasText(List<String> values) {
        return values != null && values.stream().anyMatch(item -> item != null && !item.isBlank());
    }
}
