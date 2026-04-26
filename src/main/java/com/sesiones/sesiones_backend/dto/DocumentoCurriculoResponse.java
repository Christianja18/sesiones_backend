package com.sesiones.sesiones_backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoCurriculoResponse {

    private Integer id;
    private String tipo;
    private String nombreArchivo;
    private String archivoUrl;
    private String checksumSha256;
    private String estado;
    private String errorDetalle;
    private LocalDateTime fechaSubida;
    private LocalDateTime fechaProcesado;
}

