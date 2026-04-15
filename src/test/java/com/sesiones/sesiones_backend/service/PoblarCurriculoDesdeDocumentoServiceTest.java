package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;

@ExtendWith(MockitoExtension.class)
class PoblarCurriculoDesdeDocumentoServiceTest {

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private GradoRepository gradoRepository;

    @Mock
    private CompetenciaRepository competenciaRepository;

    @Mock
    private CapacidadRepository capacidadRepository;

    @Mock
    private DesempenoRepository desempenoRepository;

    @Mock
    private PdfDocumentoCurriculoExtractorService pdfDocumentoCurriculoExtractorService;

    @InjectMocks
    private PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;

    @Test
    void shouldPopulateCompetenciasCapacidadesAndDesempenos() {
        Area area = new Area();
        area.setId(1);
        area.setNombre("Matematica");

        Grado grado = new Grado();
        grado.setId(2);
        grado.setNombre("4to");

        Competencia competencia = new Competencia();
        competencia.setId(5);
        competencia.setArea(area);
        competencia.setDescripcion("Resuelve problemas de cantidad");

        when(pdfDocumentoCurriculoExtractorService.extractText(any(byte[].class))).thenReturn(
            "Area: Matematica\n"
                + "Competencia: Resuelve problemas de cantidad\n"
                + "Capacidad: Traduce cantidades a expresiones numericas\n"
                + "Desempeno: Resuelve situaciones con fracciones en contextos cotidianos\n"
        );
        when(referenceResolver.findArea(1)).thenReturn(area);
        when(referenceResolver.findGrado(2)).thenReturn(grado);
        when(competenciaRepository.findByAreaIdAndDescripcionIgnoreCase(1, "Resuelve problemas de cantidad"))
            .thenReturn(Optional.empty());
        when(competenciaRepository.save(any(Competencia.class))).thenReturn(competencia);
        when(capacidadRepository.findByCompetenciaIdAndDescripcionIgnoreCase(5, "Traduce cantidades a expresiones numericas"))
            .thenReturn(Optional.empty());
        when(capacidadRepository.save(any(Capacidad.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcionIgnoreCase(
            2,
            5,
            "Resuelve situaciones con fracciones en contextos cotidianos"
        )).thenReturn(Optional.empty());
        when(desempenoRepository.save(any(Desempeno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoblarCurriculoDesdeDocumentoService.ResultadoPoblacion resultado =
            poblarCurriculoDesdeDocumentoService.execute("pdf".getBytes(), 1, 2);

        assertEquals(area, resultado.getArea());
        assertEquals(grado, resultado.getGrado());
        verify(competenciaRepository).save(any(Competencia.class));
        verify(capacidadRepository).save(any(Capacidad.class));
        verify(desempenoRepository).save(any(Desempeno.class));
    }
}
