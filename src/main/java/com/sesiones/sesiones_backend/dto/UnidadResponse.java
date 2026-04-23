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

    private Integer id;
    private String titulo;
    private Integer gradoId;
    private String gradoNombre;
    private Integer nivelId;
    private String nivelNombre;
    private String cicloId;
    private String cicloNombre;
    private Integer areaId;
    private String areaNombre;
    private Integer docenteId;
    private String docenteNombre;
    private Integer institucionId;
    private String institucion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String contexto;
    private LocalDateTime createdAt;
}

