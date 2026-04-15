package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.dto.GenerateSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.service.TemplateSessionGeneratorService;

@ExtendWith(MockitoExtension.class)
class GenerarSesionServiceTest {

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private CapacidadRepository capacidadRepository;

    @Mock
    private DesempenoRepository desempenoRepository;

    @Mock
    private TemplateSessionGeneratorService templateSessionGeneratorService;

    @InjectMocks
    private GenerarSesionService generarSesionService;

    @Test
    void shouldGeneratePreviewUsingResolvedCurriculum() {
        GenerateSesionRequest request = GenerateSesionRequest.builder()
            .nivelId(1L)
            .gradoId(2L)
            .areaId(3L)
            .competenciaId(4L)
            .tema("Fracciones")
            .contexto("Aula multigrado")
            .duracionMinutos(90)
            .build();

        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(1L);

        Grado grado = new Grado();
        grado.setId(2L);
        grado.setNivel(nivel);
        grado.setNombre("4to");

        Area area = new Area();
        area.setId(3L);
        area.setNombre("MatemÃ¡tica");

        Competencia competencia = new Competencia();
        competencia.setId(4L);
        competencia.setArea(area);
        competencia.setDescripcion("Resuelve problemas de cantidad");

        Capacidad capacidad = new Capacidad();
        capacidad.setId(5L);
        capacidad.setCompetencia(competencia);
        capacidad.setDescripcion("Traduce cantidades a expresiones numÃ©ricas");

        SesionResponse expected = SesionResponse.builder().titulo("SesiÃ³n generada").build();

        when(referenceResolver.findNivel(1L)).thenReturn(nivel);
        when(referenceResolver.findGradoByNivel(2L, 1L)).thenReturn(grado);
        when(referenceResolver.findArea(3L)).thenReturn(area);
        when(referenceResolver.findCompetenciaByArea(4L, 3L)).thenReturn(competencia);
        when(capacidadRepository.findByCompetenciaIdOrderByIdAsc(4L)).thenReturn(List.of(capacidad));
        when(desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(2L, 4L)).thenReturn(List.of());
        when(templateSessionGeneratorService.generate(request, grado, area, competencia, List.of(capacidad), List.of()))
            .thenReturn(expected);

        SesionResponse response = generarSesionService.execute(request);

        assertSame(expected, response);
        verify(referenceResolver).findGradoByNivel(2L, 1L);
        verify(templateSessionGeneratorService).generate(request, grado, area, competencia, List.of(capacidad), List.of());
    }
}

