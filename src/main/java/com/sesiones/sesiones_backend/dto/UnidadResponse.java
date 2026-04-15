package com.sesiones.sesiones_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnidadResponse {

    private Long id;
    private String titulo;
    private Long gradoId;
    private String gradoNombre;
    private Long nivelId;
    private String nivelNombre;
    private Long areaId;
    private String areaNombre;
    private Long docenteId;
    private String docenteNombre;
    private Long institucionId;
    private String institucion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String contexto;
    private LocalDateTime createdAt;
}
