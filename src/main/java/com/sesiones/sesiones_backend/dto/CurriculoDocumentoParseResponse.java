package com.sesiones.sesiones_backend.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurriculoDocumentoParseResponse {

    private List<NivelItem> niveles = new ArrayList<>();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NivelItem {
        private String nombre;
        private List<GradoItem> grados = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GradoItem {
        private String nombre;
        private List<AreaItem> areas = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AreaItem {
        private String nombre;
        private List<CompetenciaItem> competencias = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompetenciaItem {
        private String descripcion;
        private List<String> capacidades = new ArrayList<>();
        private List<String> desempenos = new ArrayList<>();
    }
}
