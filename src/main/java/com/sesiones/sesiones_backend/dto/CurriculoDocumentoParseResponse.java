package com.sesiones.sesiones_backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurriculoDocumentoParseResponse {

    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String area;
        private String competencia;
        private List<String> capacidades = new ArrayList<>();
        private List<String> estandares = new ArrayList<>();
        private List<String> desempenos = new ArrayList<>();
        private String nivel;
        private String grado;
        private String ciclo;
        private BigDecimal confianza;
    }
}
