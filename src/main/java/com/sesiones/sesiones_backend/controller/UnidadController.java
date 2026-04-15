package com.sesiones.sesiones_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.CreateUnidadRequest;
import com.sesiones.sesiones_backend.dto.UnidadResponse;
import com.sesiones.sesiones_backend.service.CrearUnidadService;
import com.sesiones.sesiones_backend.service.ObtenerUnidadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/unidades")
@Tag(name = "Unidades", description = "Gestión de unidades de aprendizaje")
public class UnidadController {

    private final CrearUnidadService crearUnidadService;
    private final ObtenerUnidadService obtenerUnidadService;

    @PostMapping
    @Operation(summary = "Registrar unidad")
    public ResponseEntity<UnidadResponse> create(@Valid @RequestBody CreateUnidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearUnidadService.execute(request));
    }

    @GetMapping("/{unidadId}")
    @Operation(summary = "Obtener unidad por id")
    public ResponseEntity<UnidadResponse> findById(@PathVariable Integer unidadId) {
        return ResponseEntity.ok(obtenerUnidadService.execute(unidadId));
    }
}

