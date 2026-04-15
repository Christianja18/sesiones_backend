package com.sesiones.sesiones_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.CreateDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.UpdateDocumentoCurriculoProcesamientoRequest;
import com.sesiones.sesiones_backend.service.ActualizarProcesamientoDocumentoCurriculoService;
import com.sesiones.sesiones_backend.service.ObtenerDocumentoCurriculoService;
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
    private final ActualizarProcesamientoDocumentoCurriculoService actualizarProcesamientoDocumentoCurriculoService;
    private final ObtenerDocumentoCurriculoService obtenerDocumentoCurriculoService;

    @PostMapping
    @Operation(summary = "Registrar documento curricular")
    public ResponseEntity<DocumentoCurriculoResponse> create(@Valid @RequestBody CreateDocumentoCurriculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrarDocumentoCurriculoService.execute(request));
    }

    @PatchMapping("/{documentoCurriculoId}/procesamiento")
    @Operation(summary = "Actualizar procesamiento del documento curricular")
    public ResponseEntity<DocumentoCurriculoResponse> updateProcessing(
        @PathVariable Integer documentoCurriculoId,
        @Valid @RequestBody UpdateDocumentoCurriculoProcesamientoRequest request
    ) {
        return ResponseEntity.ok(actualizarProcesamientoDocumentoCurriculoService.execute(documentoCurriculoId, request));
    }

    @GetMapping("/{documentoCurriculoId}")
    @Operation(summary = "Obtener documento curricular por id")
    public ResponseEntity<DocumentoCurriculoResponse> findById(@PathVariable Integer documentoCurriculoId) {
        return ResponseEntity.ok(obtenerDocumentoCurriculoService.execute(documentoCurriculoId));
    }
}

