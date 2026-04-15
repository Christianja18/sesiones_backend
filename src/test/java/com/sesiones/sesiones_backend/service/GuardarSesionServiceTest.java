package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesiones.sesiones_backend.dto.ActividadesSesionDto;
import com.sesiones.sesiones_backend.dto.InstrumentoEvaluacionDto;
import com.sesiones.sesiones_backend.dto.SaveSesionRequest;
import com.sesiones.sesiones_backend.dto.SesionResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.entity.Sesion;
import com.sesiones.sesiones_backend.entity.Unidad;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.SesionRepository;
import com.sesiones.sesiones_backend.service.RecursiveActivityAssembler;
import com.sesiones.sesiones_backend.service.ReferenceResolver;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;

@ExtendWith(MockitoExtension.class)
class GuardarSesionServiceTest {

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private DesempenoRepository desempenoRepository;

    private final RecursiveActivityAssembler recursiveActivityAssembler = new RecursiveActivityAssembler();
    private final SessionResponseMapper sessionResponseMapper = new SessionResponseMapper(new ObjectMapper());

    @Mock
    private SesionRepository sesionRepository;

    private GuardarSesionService guardarSesionService;

    @BeforeEach
    void setUp() {
        guardarSesionService = new GuardarSesionService(
            referenceResolver,
            desempenoRepository,
            recursiveActivityAssembler,
            sessionResponseMapper,
            sesionRepository
        );
    }

    @Test
    void shouldPersistActivitiesWithRecursiveOrder() {
        SaveSesionRequest request = SaveSesionRequest.builder()
            .unidadId(7L)
            .fecha(LocalDate.of(2026, 4, 14))
            .titulo("SesiÃ³n sobre fracciones")
            .proposito("Comprender equivalencias")
            .duracionMinutos(90)
            .competenciaIds(List.of(10L))
            .capacidadIds(List.of(20L))
            .actividades(ActividadesSesionDto.builder()
                .inicio(List.of("Recordar fracciones"))
                .desarrollo(List.of("Resolver ejercicios", "Socializar estrategias"))
                .cierre(List.of("ReflexiÃ³n final"))
                .build())
            .criteriosEvaluacion(List.of("Explica procedimientos"))
            .evidencias(List.of("Ficha resuelta"))
            .instrumentoEvaluacion(InstrumentoEvaluacionDto.builder()
                .tipo("RUBRICA")
                .detalle(List.of("Claridad", "Exactitud"))
                .build())
            .build();

        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(1L);
        nivel.setNombre("Primaria");

        Grado grado = new Grado();
        grado.setId(2L);
        grado.setNombre("4to");
        grado.setNivel(nivel);

        Unidad unidad = new Unidad();
        unidad.setId(7L);
        unidad.setTitulo("Unidad de matemÃ¡tica");
        unidad.setGrado(grado);
        Area area = new Area();
        area.setId(9L);
        area.setNombre("MatemÃƒÂ¡tica");
        unidad.setArea(area);

        Competencia competencia = new Competencia();
        competencia.setId(10L);
        competencia.setDescripcion("Resuelve problemas de cantidad");
        competencia.setArea(area);

        Capacidad capacidad = new Capacidad();
        capacidad.setId(20L);
        capacidad.setDescripcion("Usa estrategias");
        capacidad.setCompetencia(competencia);

        AtomicReference<Sesion> savedReference = new AtomicReference<>();

        when(referenceResolver.findUnidad(7L)).thenReturn(unidad);
        when(referenceResolver.findCompetencias(List.of(10L))).thenReturn(List.of(competencia));
        when(referenceResolver.findCapacidades(List.of(20L))).thenReturn(List.of(capacidad));
        when(sesionRepository.save(any(Sesion.class))).thenAnswer(invocation -> {
            Sesion sesion = invocation.getArgument(0);
            sesion.setId(55L);
            savedReference.set(sesion);
            return sesion;
        });
        when(sesionRepository.findDetailedById(55L)).thenAnswer(invocation -> Optional.of(savedReference.get()));

        SesionResponse response = guardarSesionService.execute(request);

        assertEquals(55L, response.getId());
        assertEquals(List.of("Recordar fracciones"), response.getActividades().getInicio());
        assertEquals(List.of("Resolver ejercicios", "Socializar estrategias"), response.getActividades().getDesarrollo());
        assertEquals(List.of("ReflexiÃ³n final"), response.getActividades().getCierre());
        assertEquals(List.of(1, 2, 3, 4), savedReference.get().getActividades().stream().map(item -> item.getOrden()).toList());
        verify(sesionRepository).save(any(Sesion.class));
    }
}

