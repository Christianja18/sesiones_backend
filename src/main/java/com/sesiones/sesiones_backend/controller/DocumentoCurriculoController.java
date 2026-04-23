package com.sesiones.sesiones_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.RegisterDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.service.ObtenerDocumentoCurriculoService;
import com.sesiones.sesiones_backend.service.ProcesarDocumentoCurriculoService;
import com.sesiones.sesiones_backend.service.RegistrarDocumentoCurriculoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/documentos-curriculo")
@Tag(name = "Documentos Curriculares", description = "Gestión de documentos del Currículo Nacional")
public class DocumentoCurriculoController {

    private final RegistrarDocumentoCurriculoService registrarDocumentoCurriculoService;
    private final ObtenerDocumentoCurriculoService obtenerDocumentoCurriculoService;
    private final ProcesarDocumentoCurriculoService procesarDocumentoCurriculoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar referencia de documento curricular")
    public ResponseEntity<DocumentoCurriculoResponse> create(@Valid @RequestBody RegisterDocumentoCurriculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrarDocumentoCurriculoService.execute(request));
    }

    @PostMapping("/{documentoCurriculoId}/procesar")
    @Operation(summary = "Procesar e ingerir un documento curricular registrado")
    public ResponseEntity<DocumentoCurriculoResponse> process(@PathVariable Integer documentoCurriculoId) {
        return ResponseEntity.ok(procesarDocumentoCurriculoService.execute(documentoCurriculoId));
    }

    @GetMapping("/{documentoCurriculoId}")
    @Operation(summary = "Obtener documento curricular por id")
    public ResponseEntity<DocumentoCurriculoResponse> findById(@PathVariable Integer documentoCurriculoId) {
        return ResponseEntity.ok(obtenerDocumentoCurriculoService.execute(documentoCurriculoId));
    }
}

