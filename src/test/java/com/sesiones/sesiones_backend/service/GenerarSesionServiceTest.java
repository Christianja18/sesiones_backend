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
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.service.TemplateSessionGeneratorService;

@ExtendWith(MockitoExtension.class)
class GenerarSesionServiceTest {

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private CapacidadRepository capacidadRepository;

    @Mock
    private EstandarAprendizajeRepository estandarAprendizajeRepository;

    @Mock
    private DesempenoRepository desempenoRepository;

    @Mock
    private TemplateSessionGeneratorService templateSessionGeneratorService;

    @InjectMocks
    private GenerarSesionService generarSesionService;

    @Test
    void shouldGeneratePreviewUsingResolvedCurriculum() {
        GenerateSesionRequest request = GenerateSesionRequest.builder()
            .nivelId(1)
            .gradoId(2)
            .areaId(3)
            .competenciaId(4)
            .tema("Fracciones")
            .contexto("Aula multigrado")
            .duracionMinutos(90)
            .build();

        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(1);

        Grado grado = new Grado();
        grado.setId(2);
        grado.setNivel(nivel);
        grado.setNombre("4to");

        Area area = new Area();
        area.setId(3);
        area.setNombre("MatemÃƒÆ’Ã‚Â¡tica");

        Competencia competencia = new Competencia();
        competencia.setId(4);
        competencia.setArea(area);
        competencia.setDescripcion("Resuelve problemas de cantidad");

        Capacidad capacidad = new Capacidad();
        capacidad.setId(5);
        capacidad.setCompetencia(competencia);
        capacidad.setDescripcion("Traduce cantidades a expresiones numÃƒÆ’Ã‚Â©ricas");

        SesionResponse expected = SesionResponse.builder().titulo("SesiÃƒÆ’Ã‚Â³n generada").build();

        when(referenceResolver.findNivel(1)).thenReturn(nivel);
        when(referenceResolver.findGradoByNivel(2, 1)).thenReturn(grado);
        when(referenceResolver.findArea(3)).thenReturn(area);
        when(referenceResolver.findCompetenciaByArea(4, 3)).thenReturn(competencia);
        when(capacidadRepository.findByCompetenciaIdOrderByIdAsc(4)).thenReturn(List.of(capacidad));
        when(desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(2, 4)).thenReturn(List.of());
        when(templateSessionGeneratorService.generate(request, grado, area, competencia, List.of(capacidad), List.of(), List.of()))
            .thenReturn(expected);

        SesionResponse response = generarSesionService.execute(request);

        assertSame(expected, response);
        verify(referenceResolver).findGradoByNivel(2, 1);
        verify(templateSessionGeneratorService).generate(request, grado, area, competencia, List.of(capacidad), List.of(), List.of());
    }
}

