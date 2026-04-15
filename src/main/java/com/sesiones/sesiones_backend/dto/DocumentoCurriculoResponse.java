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
    private String nombreArchivo;
    private Integer areaId;
    private String areaNombre;
    private Integer gradoId;
    private String gradoNombre;
    private Integer nivelId;
    private String nivelNombre;
    private LocalDateTime fechaSubida;
}

