package com.sesiones.sesiones_backend.dto;

import java.time.LocalDate;

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
public class CreateUnidadRequest {

    @NotBlank
    private String titulo;

    @NotNull
    private Long gradoId;

    @NotNull
    private Long areaId;

    @NotNull
    private Long docenteId;

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;

    @NotBlank
    private String contexto;
}
