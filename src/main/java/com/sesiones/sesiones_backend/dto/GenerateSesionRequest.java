package com.sesiones.sesiones_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSesionRequest {

    @NotNull
    private Integer nivelId;

    @NotNull
    private Integer gradoId;

    @NotNull
    private Integer areaId;

    @NotNull
    private Integer competenciaId;

    @NotBlank
    private String tema;

    @NotBlank
    private String contexto;

    @NotNull
    @Min(1)
    private Integer duracionMinutos;

    @Min(1)
    @Max(20)
    private Integer alternativa;
}

