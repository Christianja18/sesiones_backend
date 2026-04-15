package com.sesiones.sesiones_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextoReferenciaResponse {

    private Integer id;
    private String descripcion;
}

