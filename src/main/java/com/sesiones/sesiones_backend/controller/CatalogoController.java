package com.sesiones.sesiones_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.CatalogOptionResponse;
import com.sesiones.sesiones_backend.service.ObtenerCatalogosService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/catalogos")
@Tag(name = "Catalogos curriculares", description = "Listas de grado, area, nivel, competencia, capacidad y desempeno para combos del frontend")
public class CatalogoController {

    private final ObtenerCatalogosService obtenerCatalogosService;

    @GetMapping("/niveles")
    @Operation(
        summary = "Listar niveles educativos",
        description = "Retorna los niveles disponibles del curriculo.",
        responses = @ApiResponse(responseCode = "200", description = "Niveles listados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> niveles() {
        return ResponseEntity.ok(obtenerCatalogosService.niveles());
    }

    @GetMapping("/ciclos")
    @Operation(
        summary = "Listar ciclos",
        description = "Retorna los ciclos curriculares disponibles.",
        responses = @ApiResponse(responseCode = "200", description = "Ciclos listados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> ciclos() {
        return ResponseEntity.ok(obtenerCatalogosService.ciclos());
    }

    @GetMapping("/grados")
    @Operation(
        summary = "Listar grados",
        description = "Retorna los grados. Puede filtrarse por nivel educativo.",
        responses = @ApiResponse(responseCode = "200", description = "Grados listados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> grados(
        @Parameter(description = "ID del nivel educativo. Opcional.", example = "1")
        @RequestParam(required = false) @Positive Integer nivelId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.grados(nivelId));
    }

    @GetMapping("/areas")
    @Operation(
        summary = "Listar areas curriculares",
        description = "Retorna las areas curriculares disponibles.",
        responses = @ApiResponse(responseCode = "200", description = "Areas listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> areas() {
        return ResponseEntity.ok(obtenerCatalogosService.areas());
    }

    @GetMapping("/competencias")
    @Operation(
        summary = "Listar competencias",
        description = "Retorna las competencias. Puede filtrarse por area curricular.",
        responses = @ApiResponse(responseCode = "200", description = "Competencias listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> competencias(
        @Parameter(description = "ID del area curricular. Opcional.", example = "1")
        @RequestParam(required = false) @Positive Integer areaId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.competencias(areaId));
    }

    @GetMapping("/capacidades")
    @Operation(
        summary = "Listar capacidades",
        description = "Retorna las capacidades. Puede filtrarse por competencia.",
        responses = @ApiResponse(responseCode = "200", description = "Capacidades listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> capacidades(
        @Parameter(description = "ID de la competencia. Opcional.", example = "1")
        @RequestParam(required = false) @Positive Integer competenciaId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.capacidades(competenciaId));
    }

    @GetMapping("/estandares")
    @Operation(
        summary = "Listar estandares",
        description = "Retorna los estandares por competencia y ciclo.",
        responses = @ApiResponse(responseCode = "200", description = "Estandares listados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> estandares(
        @Parameter(description = "ID de la competencia.", required = true, example = "1")
        @RequestParam @NotNull @Positive Integer competenciaId,
        @Parameter(description = "ID del ciclo.", required = true, example = "III")
        @RequestParam @NotBlank String cicloId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.estandares(competenciaId, cicloId));
    }

    @GetMapping("/desempenos")
    @Operation(
        summary = "Listar desempenos",
        description = "Retorna los desempenos. Puede filtrarse por grado, competencia o ambos.",
        responses = @ApiResponse(responseCode = "200", description = "Desempenos listados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> desempenos(
        @Parameter(description = "ID del grado. Opcional.", example = "2")
        @RequestParam(required = false) @Positive Integer gradoId,
        @Parameter(description = "ID de la competencia. Opcional.", example = "1")
        @RequestParam(required = false) @Positive Integer competenciaId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.desempenos(gradoId, competenciaId));
    }

    @GetMapping("/docentes")
    @Operation(
        summary = "Listar docentes activos",
        description = "Retorna docentes activos para seleccion de responsable.",
        responses = @ApiResponse(responseCode = "200", description = "Docentes listados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> docentes() {
        return ResponseEntity.ok(obtenerCatalogosService.docentes());
    }

    @GetMapping("/unidades")
    @Operation(
        summary = "Listar unidades por docente",
        description = "Retorna unidades registradas para un docente.",
        responses = @ApiResponse(responseCode = "200", description = "Unidades listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> unidades(
        @Parameter(description = "ID del docente.", required = true, example = "1")
        @RequestParam @NotNull @Positive Integer docenteId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.unidades(docenteId));
    }

    @GetMapping("/sesiones")
    @Operation(
        summary = "Listar sesiones por unidad",
        description = "Retorna sesiones registradas para una unidad.",
        responses = @ApiResponse(responseCode = "200", description = "Sesiones listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionResponse.class))))
    )
    public ResponseEntity<List<CatalogOptionResponse>> sesiones(
        @Parameter(description = "ID de la unidad.", required = true, example = "1")
        @RequestParam @NotNull @Positive Integer unidadId
    ) {
        return ResponseEntity.ok(obtenerCatalogosService.sesiones(unidadId));
    }
}
