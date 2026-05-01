package com.sesiones.sesiones_backend.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Opcion de catalogo para combos del frontend")
public class CatalogOptionResponse {

    @Schema(description = "Identificador del registro", example = "1")
    private String id;

    @Schema(description = "Texto visible para mostrar en el combo", example = "Primaria")
    private String nombre;
}
