package com.sesiones.sesiones_backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveSesionRequest {

    @NotNull
    private Long unidadId;

    @NotNull
    private LocalDate fecha;

    @NotBlank
    private String titulo;

    @NotBlank
    private String proposito;

    @NotNull
    private Integer duracionMinutos;

    private boolean generadoPorIa;

    @Builder.Default
    private List<Long> competenciaIds = new ArrayList<>();

    @Builder.Default
    private List<Long> capacidadIds = new ArrayList<>();

    @Builder.Default
    private List<Long> desempenoIds = new ArrayList<>();

    @Valid
    @NotNull
    private ActividadesSesionDto actividades;

    @Builder.Default
    private List<String> criteriosEvaluacion = new ArrayList<>();

    @Builder.Default
    private List<String> evidencias = new ArrayList<>();

    @Valid
    @NotNull
    private InstrumentoEvaluacionDto instrumentoEvaluacion;
}
