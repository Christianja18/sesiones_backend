package com.sesiones.sesiones_backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionResponse {

    private Integer id;
    private Integer unidadId;
    private String unidadTitulo;
    private LocalDate fecha;
    private String titulo;
    private String proposito;
    private Integer duracionMinutos;
    private boolean generadoPorIa;

    @Builder.Default
    private List<TextoReferenciaResponse> competencias = new ArrayList<>();

    @Builder.Default
    private List<TextoReferenciaResponse> capacidades = new ArrayList<>();

    @Builder.Default
    private List<TextoReferenciaResponse> desempenos = new ArrayList<>();

    @Builder.Default
    private ActividadesSesionDto actividades = ActividadesSesionDto.builder().build();

    @Builder.Default
    private List<String> criteriosEvaluacion = new ArrayList<>();

    @Builder.Default
    private List<String> evidencias = new ArrayList<>();

    private InstrumentoEvaluacionDto instrumentoEvaluacion;
}

