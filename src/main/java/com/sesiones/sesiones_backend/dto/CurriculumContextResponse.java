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

    private Long nivelId;
    private String nivelNombre;
    private Long gradoId;
    private String gradoNombre;
    private Long areaId;
    private String areaNombre;
    private Long competenciaId;
    private String competenciaDescripcion;

    @Builder.Default
    private List<TextoReferenciaResponse> capacidades = new ArrayList<>();

    @Builder.Default
    private List<TextoReferenciaResponse> desempenos = new ArrayList<>();
}
