package com.sesiones.sesiones_backend.dto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSesionRequest {

    @NotNull
    private Integer nivelId;

    @NotNull
    private Integer gradoId;

    @NotNull
    private Integer areaId;

    @Min(1)
    @Schema(
        description = "Campo legado para seleccionar una sola competencia. Usar competenciaIds para seleccion multiple.",
        deprecated = true
    )
    private Integer competenciaId;

    @Builder.Default
    @Schema(description = "Competencias seleccionadas para generar la sesion. Permite una o mas competencias del area.")
    private List<@NotNull @Min(1) Integer> competenciaIds = new ArrayList<>();

    @NotBlank
    private String tema;

    @NotBlank
    private String contexto;

    @NotNull
    @Min(1)
    private Integer duracionMinutos;

    @Min(1)
    @Max(20)
    private Integer alternativa;

    @JsonIgnore
    @AssertTrue(message = "Selecciona al menos una competencia")
    public boolean isCompetenciaSelectionValid() {
        return !selectedCompetenciaIds().isEmpty();
    }

    @JsonIgnore
    public List<Integer> selectedCompetenciaIds() {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        if (competenciaIds != null) {
            competenciaIds.stream()
                .filter(id -> id != null && id > 0)
                .forEach(ids::add);
        }
        if (ids.isEmpty() && competenciaId != null && competenciaId > 0) {
            ids.add(competenciaId);
        }
        return new ArrayList<>(ids);
    }
}

