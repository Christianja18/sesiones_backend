package com.sesiones.sesiones_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocenteResponse {

    private Integer id;
    private String nombre;
    private String email;
    private Integer institucionId;
    private String institucion;
}

