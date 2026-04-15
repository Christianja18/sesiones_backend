package com.sesiones.sesiones_backend.dto;

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
public class GenerateSesionRequest {

    @NotNull
    private Long nivelId;

    @NotNull
    private Long gradoId;

    @NotNull
    private Long areaId;

    @NotNull
    private Long competenciaId;

    @NotBlank
    private String tema;

    @NotBlank
    private String contexto;

    @NotNull
    private Integer duracionMinutos;
}
