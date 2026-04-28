package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.GenerateSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Ciclo;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;

@ExtendWith(MockitoExtension.class)
class GenerarSesionIAServiceTest {

    @Mock
    private LLMClient llmClient;

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private CapacidadRepository capacidadRepository;

    @Mock
    private DesempenoRepository desempenoRepository;

    @Mock
    private EstandarAprendizajeRepository estandarAprendizajeRepository;

    @Mock
    private TemplateSessionGeneratorService templateSessionGeneratorService;

    private GenerarSesionIAService generarSesionIAService;

    @BeforeEach
    void setUp() {
        generarSesionIAService = new GenerarSesionIAService(
            llmClient,
            referenceResolver,
            capacidadRepository,
            desempenoRepository,
            estandarAprendizajeRepository,
            templateSessionGeneratorService,
            new ObjectMapper()
        );
    }

    @Test
    void shouldGenerateSessionWithDeepSeekResponse() {
        GenerateSesionRequest request = buildRequest();
        NivelEducativo nivel = buildNivel();
        Grado grado = buildGrado(nivel);
        Area area = buildArea();
        Competencia competencia = buildCompetencia(area);
        Capacidad capacidad = buildCapacidad(competencia);
        Desempeno desempeno = buildDesempeno(grado, competencia);

        when(referenceResolver.findNivel(1)).thenReturn(nivel);
        when(referenceResolver.findGradoByNivel(2, 1)).thenReturn(grado);
        when(referenceResolver.findArea(3)).thenReturn(area);
        when(referenceResolver.findCompetenciaByArea(4, 3)).thenReturn(competencia);
        when(capacidadRepository.findByCompetenciaIdOrderByIdAsc(4)).thenReturn(List.of(capacidad));
        when(estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(4, "IV")).thenReturn(List.of());
        when(desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(2, 4)).thenReturn(List.of(desempeno));
        when(llmClient.generate(anyString())).thenReturn("""
            {
              "titulo": "Sesion sobre fracciones equivalentes",
              "proposito": "Desarrollar la comprension de fracciones equivalentes.",
              "competencias": ["Resuelve problemas de cantidad"],
              "capacidades": ["Traduce cantidades a expresiones numericas"],
              "desempenos": ["Explica equivalencias entre fracciones"]
            }
            """, """
            {
              "inicio": ["Recuperar saberes previos sobre fracciones."],
              "desarrollo": ["Resolver problemas en parejas.", "Explicar estrategias al grupo."],
              "cierre": ["Reflexionar sobre el uso de fracciones en la vida diaria."]
            }
            """, """
            {
              "criterios": ["Explica equivalencias con claridad."],
              "evidencias": ["Ficha de trabajo resuelta."],
              "instrumento": {
                "tipo": "rubrica",
                "detalle": ["Claridad conceptual", "Aplicacion en problemas"]
              }
            }
            """);

        SesionResponse response = generarSesionIAService.execute(request);

        assertTrue(response.isGeneradoPorIa());
        assertEquals("Sesion sobre fracciones equivalentes", response.getTitulo());
        assertEquals(List.of("Recuperar saberes previos sobre fracciones."), response.getActividades().getInicio());
        assertEquals("rubrica", response.getInstrumentoEvaluacion().getTipo());
        assertEquals(List.of("Claridad conceptual", "Aplicacion en problemas"), response.getInstrumentoEvaluacion().getDetalle());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, times(3)).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getAllValues().get(0).contains("- Nivel: Primaria"));
        assertTrue(promptCaptor.getAllValues().get(0).contains("- Ciclo: IV - Ciclo IV"));
        assertTrue(promptCaptor.getAllValues().get(0).contains("- Area: Matematica"));
        assertTrue(promptCaptor.getAllValues().get(0).contains("- Enfoque de esta alternativa:"));
    }

    @Test
    void shouldFallbackToTemplateAfterInvalidResponses() {
        GenerateSesionRequest request = buildRequest();
        NivelEducativo nivel = buildNivel();
        Grado grado = buildGrado(nivel);
        Area area = buildArea();
        Competencia competencia = buildCompetencia(area);
        Capacidad capacidad = buildCapacidad(competencia);

        SesionResponse fallbackResponse = SesionResponse.builder()
            .titulo("Sesion de plantilla")
            .generadoPorIa(false)
            .build();

        when(referenceResolver.findNivel(1)).thenReturn(nivel);
        when(referenceResolver.findGradoByNivel(2, 1)).thenReturn(grado);
        when(referenceResolver.findArea(3)).thenReturn(area);
        when(referenceResolver.findCompetenciaByArea(4, 3)).thenReturn(competencia);
        when(capacidadRepository.findByCompetenciaIdOrderByIdAsc(4)).thenReturn(List.of(capacidad));
        when(estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdOrderByIdAsc(4, "IV")).thenReturn(Collections.emptyList());
        when(desempenoRepository.findByGradoIdAndCompetenciaIdOrderByIdAsc(2, 4)).thenReturn(Collections.emptyList());
        when(llmClient.generate(anyString())).thenReturn("no-json", "no-json");
        when(templateSessionGeneratorService.generate(request, grado, area, competencia, List.of(capacidad), Collections.emptyList(), Collections.emptyList()))
            .thenReturn(fallbackResponse);

        SesionResponse response = generarSesionIAService.execute(request);

        assertSame(fallbackResponse, response);
        assertFalse(response.isGeneradoPorIa());
        verify(llmClient, times(2)).generate(anyString());
    }

    private GenerateSesionRequest buildRequest() {
        return GenerateSesionRequest.builder()
            .nivelId(1)
            .gradoId(2)
            .areaId(3)
            .competenciaId(4)
            .tema("Fracciones")
            .contexto("Aula multigrado")
            .duracionMinutos(90)
            .build();
    }

    private NivelEducativo buildNivel() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(1);
        nivel.setNombre("Primaria");
        return nivel;
    }

    private Grado buildGrado(NivelEducativo nivel) {
        Grado grado = new Grado();
        grado.setId(2);
        grado.setNombre("4to");
        grado.setNivel(nivel);
        grado.setCiclo(buildCiclo());
        return grado;
    }

    private Ciclo buildCiclo() {
        Ciclo ciclo = new Ciclo();
        ciclo.setId("IV");
        ciclo.setNombre("Ciclo IV");
        return ciclo;
    }

    private Area buildArea() {
        Area area = new Area();
        area.setId(3);
        area.setNombre("Matematica");
        return area;
    }

    private Competencia buildCompetencia(Area area) {
        Competencia competencia = new Competencia();
        competencia.setId(4);
        competencia.setArea(area);
        competencia.setDescripcion("Resuelve problemas de cantidad");
        return competencia;
    }

    private Capacidad buildCapacidad(Competencia competencia) {
        Capacidad capacidad = new Capacidad();
        capacidad.setId(5);
        capacidad.setCompetencia(competencia);
        capacidad.setDescripcion("Traduce cantidades a expresiones numericas");
        return capacidad;
    }

    private Desempeno buildDesempeno(Grado grado, Competencia competencia) {
        Desempeno desempeno = new Desempeno();
        desempeno.setId(6);
        desempeno.setGrado(grado);
        desempeno.setCompetencia(competencia);
        desempeno.setDescripcion("Explica equivalencias entre fracciones");
        return desempeno;
    }
}
