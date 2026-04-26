package com.sesiones.sesiones_backend.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumContextResponse {

    private Integer nivelId;
    private String nivelNombre;
    private String cicloId;
    private String cicloNombre;
    private Integer gradoId;
    private String gradoNombre;
    private Integer areaId;
    private String areaNombre;
    private Integer competenciaId;
    private String competenciaDescripcion;

    @Builder.Default
    private List<TextoReferenciaResponse> capacidades = new ArrayList<>();

    @Builder.Default
    private List<TextoReferenciaResponse> estandares = new ArrayList<>();

    @Builder.Default
    private List<TextoReferenciaResponse> desempenos = new ArrayList<>();
}

