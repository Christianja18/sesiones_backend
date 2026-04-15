package com.sesiones.sesiones_backend.mapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoProcesamientoResponse;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.InstrumentoEvaluacionDto;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.dto.TextoReferenciaResponse;
import com.sesiones.sesiones_backend.dto.UnidadResponse;
import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.util.enums.ActividadTipo;
import com.sesiones.sesiones_backend.entity.Sesion;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionResponseMapper {

    private final ObjectMapper objectMapper;

    public UnidadResponse toUnidadResponse(Unidad unidad) {
        return UnidadResponse.builder()
            .id(unidad.getId())
            .titulo(unidad.getTitulo())
            .gradoId(unidad.getGrado().getId())
            .gradoNombre(unidad.getGrado().getNombre())
            .nivelId(unidad.getGrado().getNivel().getId())
            .nivelNombre(unidad.getGrado().getNivel().getNombre())
            .areaId(unidad.getArea().getId())
            .areaNombre(unidad.getArea().getNombre())
            .docenteId(unidad.getDocente().getId())
            .docenteNombre(unidad.getDocente().getNombre())
            .institucionId(unidad.getDocente().getInstitucion().getId())
            .institucion(unidad.getDocente().getInstitucion().getNombre())
            .fechaInicio(unidad.getFechaInicio())
            .fechaFin(unidad.getFechaFin())
            .contexto(unidad.getContexto())
            .createdAt(unidad.getCreatedAt())
            .build();
    }

    public SesionResponse toSesionResponse(Sesion sesion) {
        return SesionResponse.builder()
            .id(sesion.getId())
            .unidadId(sesion.getUnidad().getId())
            .unidadTitulo(sesion.getUnidad().getTitulo())
            .fecha(sesion.getFecha())
            .titulo(sesion.getTitulo())
            .proposito(sesion.getProposito())
            .duracionMinutos(sesion.getDuracionMinutos())
            .generadoPorIa(sesion.isGeneradoPorIa())
            .competencias(sesion.getCompetencias().stream().map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion())).toList())
            .capacidades(sesion.getCapacidades().stream().map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion())).toList())
            .desempenos(sesion.getDesempenos().stream().map(item -> new TextoReferenciaResponse(item.getId(), item.getDescripcion())).toList())
            .actividades(toActividades(sesion))
            .criteriosEvaluacion(sesion.getCriteriosEvaluacion().stream().map(item -> item.getDescripcion()).toList())
            .evidencias(sesion.getEvidencias().stream().map(item -> item.getDescripcion()).toList())
            .instrumentoEvaluacion(toInstrumento(sesion))
            .build();
    }

    public String toInstrumentJson(InstrumentoEvaluacionDto instrumento) {
        try {
            InstrumentoTipo normalizedType = InstrumentoTipo.fromDatabaseValue(instrumento.getTipo());
            return objectMapper.writeValueAsString(Map.of(
                "tipo", normalizedType.getDatabaseValue(),
                "detalle", safeList(instrumento.getDetalle())
            ));
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible serializar el instrumento de evaluaciÃ³n");
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("El tipo de instrumento debe ser rubrica o lista_cotejo");
        }
    }

    public DocumentoCurriculoResponse toDocumentoCurriculoResponse(DocumentoCurriculo documentoCurriculo) {
        return DocumentoCurriculoResponse.builder()
            .id(documentoCurriculo.getId())
            .nombreArchivo(documentoCurriculo.getNombreArchivo())
            .rutaArchivo(documentoCurriculo.getRutaArchivo())
            .areaId(documentoCurriculo.getArea() == null ? null : documentoCurriculo.getArea().getId())
            .areaNombre(documentoCurriculo.getArea() == null ? null : documentoCurriculo.getArea().getNombre())
            .gradoId(documentoCurriculo.getGrado() == null ? null : documentoCurriculo.getGrado().getId())
            .gradoNombre(documentoCurriculo.getGrado() == null ? null : documentoCurriculo.getGrado().getNombre())
            .nivelId(documentoCurriculo.getGrado() == null ? null : documentoCurriculo.getGrado().getNivel().getId())
            .nivelNombre(documentoCurriculo.getGrado() == null ? null : documentoCurriculo.getGrado().getNivel().getNombre())
            .fechaSubida(documentoCurriculo.getFechaSubida())
            .procesamiento(toDocumentoProcesamientoResponse(documentoCurriculo))
            .build();
    }

    private ActividadesSesionDto toActividades(Sesion sesion) {
        return ActividadesSesionDto.builder()
            .inicio(filterActivitiesByType(sesion, ActividadTipo.INICIO))
            .desarrollo(filterActivitiesByType(sesion, ActividadTipo.DESARROLLO))
            .cierre(filterActivitiesByType(sesion, ActividadTipo.CIERRE))
            .build();
    }

    private InstrumentoEvaluacionDto toInstrumento(Sesion sesion) {
        if (sesion.getInstrumentosEvaluacion().isEmpty()) {
            return null;
        }

        String rawJson = sesion.getInstrumentosEvaluacion().get(0).getContenidoJson();
        try {
            Map<String, Object> payload = objectMapper.readValue(rawJson, new TypeReference<>() {});
            Object detail = payload.get("detalle");
            return InstrumentoEvaluacionDto.builder()
                .tipo(String.valueOf(payload.get("tipo")))
                .detalle(detail instanceof List<?> list ? list.stream().map(String::valueOf).toList() : Collections.emptyList())
                .build();
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible deserializar el instrumento de evaluaciÃ³n");
        }
    }

    private List<String> filterActivitiesByType(Sesion sesion, ActividadTipo tipo) {
        return sesion.getActividades().stream()
            .filter(item -> item.getTipo() == tipo)
            .map(item -> item.getDescripcion())
            .toList();
    }

    private DocumentoCurriculoProcesamientoResponse toDocumentoProcesamientoResponse(DocumentoCurriculo documentoCurriculo) {
        if (documentoCurriculo.getProcesamiento() == null) {
            return null;
        }

        return DocumentoCurriculoProcesamientoResponse.builder()
            .id(documentoCurriculo.getProcesamiento().getId())
            .estado(documentoCurriculo.getProcesamiento().getEstado().getDatabaseValue())
            .observacion(documentoCurriculo.getProcesamiento().getObservacion())
            .fechaUltimoProceso(documentoCurriculo.getProcesamiento().getFechaUltimoProceso())
            .createdAt(documentoCurriculo.getProcesamiento().getCreatedAt())
            .updatedAt(documentoCurriculo.getProcesamiento().getUpdatedAt())
            .build();
    }

    private List<String> safeList(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .toList();
    }
}

