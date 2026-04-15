package com.sesiones.sesiones_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.CurriculumContextResponse;
import com.sesiones.sesiones_backend.service.ObtenerContextoCurricularService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/curriculum")
@Tag(name = "Curriculo", description = "Consulta de contexto curricular")
public class CurriculumController {

    private final ObtenerContextoCurricularService obtenerContextoCurricularService;

    @GetMapping("/contexto")
    @Operation(summary = "Obtener contexto curricular por nivel, grado, area y competencia")
    public ResponseEntity<CurriculumContextResponse> getContext(
        @RequestParam Long nivelId,
        @RequestParam Long gradoId,
        @RequestParam Long areaId,
        @RequestParam Long competenciaId
    ) {
        return ResponseEntity.ok(obtenerContextoCurricularService.execute(nivelId, gradoId, areaId, competenciaId));
    }
}
