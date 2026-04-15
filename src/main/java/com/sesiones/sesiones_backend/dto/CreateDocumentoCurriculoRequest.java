package com.sesiones.sesiones_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentoCurriculoRequest {

    @NotBlank
    private String nombreArchivo;

    @NotBlank
    private String rutaArchivo;

    private Long areaId;

    private Long gradoId;
}
