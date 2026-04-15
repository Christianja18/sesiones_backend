package com.sesiones.sesiones_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.CreateDocenteRequest;
import com.sesiones.sesiones_backend.dto.DocenteResponse;
import com.sesiones.sesiones_backend.service.CrearDocenteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/docentes")
@Tag(name = "Docentes", description = "Gestión de docentes")
public class DocenteController {

    private final CrearDocenteService crearDocenteService;

    @PostMapping
    @Operation(summary = "Registrar docente")
    public ResponseEntity<DocenteResponse> create(@Valid @RequestBody CreateDocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearDocenteService.execute(request));
    }
}
