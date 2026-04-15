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

    private Long id;
    private String nombreArchivo;
    private String rutaArchivo;
    private Long areaId;
    private String areaNombre;
    private Long gradoId;
    private String gradoNombre;
    private Long nivelId;
    private String nivelNombre;
    private LocalDateTime fechaSubida;
    private DocumentoCurriculoProcesamientoResponse procesamiento;
}
