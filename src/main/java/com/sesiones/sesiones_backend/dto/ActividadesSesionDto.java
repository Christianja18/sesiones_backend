package com.sesiones.sesiones_backend.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadesSesionDto {

    @Builder.Default
    private List<String> inicio = new ArrayList<>();

    @Builder.Default
    private List<String> desarrollo = new ArrayList<>();

    @Builder.Default
    private List<String> cierre = new ArrayList<>();
}
