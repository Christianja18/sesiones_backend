package com.sesiones.sesiones_backend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentoCurriculoProcesamientoRequest {

    @NotBlank
    private String estado;

    private String observacion;

    private LocalDateTime fechaUltimoProceso;
}
