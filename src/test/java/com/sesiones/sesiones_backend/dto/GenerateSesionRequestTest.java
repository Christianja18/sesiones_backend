package com.sesiones.sesiones_backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class GenerateSesionRequestTest {

    @Test
    void selectedCompetenciaIdsShouldUseMultipleIdsWithoutDuplicates() {
        GenerateSesionRequest request = GenerateSesionRequest.builder()
            .competenciaId(99)
            .competenciaIds(List.of(4, 7, 4, 9))
            .build();

        assertEquals(List.of(4, 7, 9), request.selectedCompetenciaIds());
    }

    @Test
    void selectedCompetenciaIdsShouldFallbackToLegacyCompetenciaId() {
        GenerateSesionRequest request = GenerateSesionRequest.builder()
            .competenciaId(4)
            .build();

        assertEquals(List.of(4), request.selectedCompetenciaIds());
    }
}
