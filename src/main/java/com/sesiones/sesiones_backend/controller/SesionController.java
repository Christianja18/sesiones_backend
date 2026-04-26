package com.sesiones.sesiones_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.GenerateSesionRequest;
import com.sesiones.sesiones_backend.dto.SaveSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.service.GenerarSesionIAService;
import com.sesiones.sesiones_backend.service.GenerarSesionService;
import com.sesiones.sesiones_backend.service.GuardarSesionService;
import com.sesiones.sesiones_backend.service.ObtenerSesionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sesiones")
@Tag(name = "Sesiones", description = "Generacion y persistencia de sesiones de aprendizaje")
public class SesionController {

    private final GenerarSesionIAService generarSesionIAService;
    private final GenerarSesionService generarSesionService;
    private final GuardarSesionService guardarSesionService;
    private final ObtenerSesionService obtenerSesionService;

    @PostMapping("/generar")
    @Operation(summary = "Generar borrador de sesion con IA")
    public ResponseEntity<SesionResponse> generate(@Valid @RequestBody GenerateSesionRequest request) {
        return ResponseEntity.ok(generarSesionIAService.execute(request));
    }

    @PostMapping("/generar-plantilla")
    @Operation(summary = "Generar borrador de sesion con plantilla local")
    public ResponseEntity<SesionResponse> generateTemplate(@Valid @RequestBody GenerateSesionRequest request) {
        return ResponseEntity.ok(generarSesionService.execute(request));
    }

    @PostMapping
    @Operation(summary = "Guardar sesion")
    public ResponseEntity<SesionResponse> save(@Valid @RequestBody SaveSesionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardarSesionService.execute(request));
    }

    @GetMapping("/{sesionId}")
    @Operation(summary = "Obtener sesion por id")
    public ResponseEntity<SesionResponse> findById(@PathVariable Integer sesionId) {
        return ResponseEntity.ok(obtenerSesionService.execute(sesionId));
    }
}
