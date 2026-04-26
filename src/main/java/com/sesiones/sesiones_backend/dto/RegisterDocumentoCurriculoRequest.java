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
public class RegisterDocumentoCurriculoRequest {

    @NotBlank
    private String tipo;

    @NotBlank
    private String nombreArchivo;

    @NotBlank
    private String archivoUrl;
}
