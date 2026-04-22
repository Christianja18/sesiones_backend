package com.sesiones.sesiones_backend.mapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.InstrumentoEvaluacionDto;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.dto.TextoReferenciaResponse;
import com.sesiones.sesiones_backend.dto.UnidadResponse;
import com.sesiones.sesiones_backend.entity.Actividad;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.InstrumentoEvaluacion;
import com.sesiones.sesiones_backend.entity.Sesion;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.util.enums.ActividadTipo;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionResponseMapper {

    private final ObjectMapper objectMapper;

    public UnidadResponse toUnidadResponse(Unidad unidad) {
        if (unidad == null) {
            return null;
        }

        Grado grado = unidad.getGrado();
        var nivel = grado == null ? null : grado.getNivel();
        var area = unidad.getArea();
        var docente = unidad.getDocente();
        var institucion = docente == null ? null : docente.getInstitucion();

        return UnidadResponse.builder()
            .id(unidad.getId())
            .titulo(unidad.getTitulo())
            .gradoId(grado == null ? null : grado.getId())
            .gradoNombre(grado == null ? null : grado.getNombre())
            .nivelId(nivel == null ? null : nivel.getId())
            .nivelNombre(nivel == null ? null : nivel.getNombre())
            .areaId(area == null ? null : area.getId())
            .areaNombre(area == null ? null : area.getNombre())
            .docenteId(docente == null ? null : docente.getId())
            .docenteNombre(docente == null ? null : docente.getNombre())
            .institucionId(institucion == null ? null : institucion.getId())
            .institucion(institucion == null ? null : institucion.getNombre())
            .fechaInicio(unidad.getFechaInicio())
            .fechaFin(unidad.getFechaFin())
            .contexto(unidad.getContexto())
            .createdAt(unidad.getCreatedAt())
            .build();
    }

    public SesionResponse toSesionResponse(Sesion sesion) {
        if (sesion == null) {
            return null;
        }

        Unidad unidad = sesion.getUnidad();
        return SesionResponse.builder()
            .id(sesion.getId())
            .unidadId(unidad == null ? null : unidad.getId())
            .unidadTitulo(unidad == null ? null : unidad.getTitulo())
            .fecha(sesion.getFecha())
            .titulo(sesion.getTitulo())
            .proposito(sesion.getProposito())
            .duracionMinutos(sesion.getDuracionMinutos())
            .generadoPorIa(sesion.isGeneradoPorIa())
            .competencias(mapReferences(sesion.getCompetencias() == null ? Collections.emptyList() : sesion.getCompetencias().stream()
                .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
                .collect(Collectors.toList())))
            .capacidades(mapReferences(sesion.getCapacidades() == null ? Collections.emptyList() : sesion.getCapacidades().stream()
                .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
                .collect(Collectors.toList())))
            .desempenos(mapReferences(sesion.getDesempenos() == null ? Collections.emptyList() : sesion.getDesempenos().stream()
                .map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion()))
                .collect(Collectors.toList())))
            .actividades(toActividades(sesion))
            .criteriosEvaluacion(safeTextList(sesion.getCriteriosEvaluacion() == null ? Collections.emptyList() : sesion.getCriteriosEvaluacion().stream()
                .map(item -> item.getDescripcion())
                .collect(Collectors.toList())))
            .evidencias(safeTextList(sesion.getEvidencias() == null ? Collections.emptyList() : sesion.getEvidencias().stream()
                .map(item -> item.getDescripcion())
                .collect(Collectors.toList())))
            .instrumentoEvaluacion(toInstrumento(sesion))
            .build();
    }

    public String toInstrumentJson(InstrumentoEvaluacionDto instrumento) {
        if (instrumento == null) {
            throw new BusinessRuleException("El instrumento de evaluacion es obligatorio");
        }

        try {
            InstrumentoTipo.fromDatabaseValue(instrumento.getTipo());
            return objectMapper.writeValueAsString(Map.of(
                "detalle", safeTextList(instrumento.getDetalle())
            ));
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible serializar el instrumento de evaluacion");
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("El tipo de instrumento debe ser rubrica o lista_cotejo");
        }
    }

    public DocumentoCurriculoResponse toDocumentoCurriculoResponse(DocumentoCurriculo documentoCurriculo) {
        if (documentoCurriculo == null) {
            return null;
        }

        return DocumentoCurriculoResponse.builder()
            .id(documentoCurriculo.getId())
            .nombreArchivo(documentoCurriculo.getNombreArchivo())
            .archivoUrl(documentoCurriculo.getArchivoUrl())
            .checksumSha256(documentoCurriculo.getChecksumSha256())
            .estado(documentoCurriculo.getEstado() == null ? null : documentoCurriculo.getEstado().getDatabaseValue())
            .errorDetalle(documentoCurriculo.getErrorDetalle())
            .fechaSubida(documentoCurriculo.getFechaSubida())
            .fechaProcesado(documentoCurriculo.getFechaProcesado())
            .build();
    }

    private ActividadesSesionDto toActividades(Sesion sesion) {
        List<Actividad> orderedActivities = sesion.getActividades() == null
            ? Collections.emptyList()
            : sesion.getActividades().stream()
                .filter(item -> item != null)
                .sorted(Comparator.comparing(Actividad::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        return ActividadesSesionDto.builder()
            .inicio(filterActivitiesByType(orderedActivities, ActividadTipo.INICIO))
            .desarrollo(filterActivitiesByType(orderedActivities, ActividadTipo.DESARROLLO))
            .cierre(filterActivitiesByType(orderedActivities, ActividadTipo.CIERRE))
            .build();
    }

    private InstrumentoEvaluacionDto toInstrumento(Sesion sesion) {
        if (sesion.getInstrumentosEvaluacion() == null || sesion.getInstrumentosEvaluacion().isEmpty()) {
            return null;
        }

        InstrumentoEvaluacion instrumento = sesion.getInstrumentosEvaluacion().stream()
            .filter(item -> item != null)
            .findFirst()
            .orElse(null);
        if (instrumento == null) {
            return null;
        }

        if (instrumento.getContenidoJson() == null || instrumento.getContenidoJson().isBlank()) {
            return InstrumentoEvaluacionDto.builder()
                .tipo(instrumento.getTipo() == null ? null : instrumento.getTipo().getDatabaseValue())
                .detalle(Collections.emptyList())
                .build();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(instrumento.getContenidoJson(), new TypeReference<>() {});
            Object detail = payload.get("detalle");
            List<String> detalle = Collections.emptyList();
            if (detail instanceof List<?>) {
                detalle = ((List<?>) detail).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            }
            return InstrumentoEvaluacionDto.builder()
                .tipo(instrumento.getTipo() == null ? null : instrumento.getTipo().getDatabaseValue())
                .detalle(detalle)
                .build();
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible deserializar el instrumento de evaluacion");
        }
    }

    private List<String> filterActivitiesByType(List<Actividad> actividades, ActividadTipo tipo) {
        return actividades.stream()
            .filter(item -> item.getTipo() == tipo)
            .map(Actividad::getDescripcion)
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private List<TextoReferenciaResponse> mapReferences(List<TextoReferenciaResponse> references) {
        if (references == null || references.isEmpty()) {
            return Collections.emptyList();
        }

        return references.stream()
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }

    private List<String> safeTextList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .collect(Collectors.toList());
    }
}
