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
public class DocumentoCurriculoProcesamientoResponse {

    private Integer id;
    private String estado;
    private String observacion;
    private LocalDateTime fechaUltimoProceso;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

