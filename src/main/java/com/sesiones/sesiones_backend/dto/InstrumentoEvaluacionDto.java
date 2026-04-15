package com.sesiones.sesiones_backend.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentoEvaluacionDto {

    @NotBlank
    private String tipo;

    @Builder.Default
    private List<String> detalle = new ArrayList<>();
}
