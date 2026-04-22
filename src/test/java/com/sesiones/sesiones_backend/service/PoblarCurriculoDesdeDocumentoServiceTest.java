package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;

@ExtendWith(MockitoExtension.class)
class PoblarCurriculoDesdeDocumentoServiceTest {

    @Mock
    private PdfTextExtractorService pdfTextExtractorService;

    @Mock
    private CurriculoLlmClient curriculoLlmClient;

    @Mock
    private NivelEducativoRepository nivelEducativoRepository;

    @Mock
    private GradoRepository gradoRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private CompetenciaRepository competenciaRepository;

    @Mock
    private CapacidadRepository capacidadRepository;

    @Mock
    private DesempenoRepository desempenoRepository;

    @InjectMocks
    private PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;

    @Test
    void shouldPopulateCurriculumEntitiesFromPdf() {
        CurriculoDocumentoParseResponse response = buildResponse();
        when(pdfTextExtractorService.extractText(any(byte[].class))).thenReturn("texto curricular");
        when(curriculoLlmClient.extraerCurriculo("texto curricular", null, null, null)).thenReturn(response);

        when(nivelEducativoRepository.findByNombreIgnoreCase("Primaria")).thenReturn(Optional.empty());
        when(nivelEducativoRepository.save(any(NivelEducativo.class))).thenAnswer(invocation -> {
            NivelEducativo nivel = invocation.getArgument(0);
            nivel.setId(1);
            return nivel;
        });

        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(1, "1ro")).thenReturn(Optional.empty());
        when(gradoRepository.save(any(Grado.class))).thenAnswer(invocation -> {
            Grado grado = invocation.getArgument(0);
            grado.setId(2);
            return grado;
        });

        when(areaRepository.findByNombreIgnoreCase("Matematica")).thenReturn(Optional.empty());
        when(areaRepository.save(any(Area.class))).thenAnswer(invocation -> {
            Area area = invocation.getArgument(0);
            area.setId(3);
            return area;
        });

        when(competenciaRepository.findByAreaIdAndDescripcion(3, "Resuelve problemas de cantidad")).thenReturn(Optional.empty());
        when(competenciaRepository.save(any(Competencia.class))).thenAnswer(invocation -> {
            Competencia competencia = invocation.getArgument(0);
            competencia.setId(4);
            return competencia;
        });

        when(capacidadRepository.findByCompetenciaIdAndDescripcion(4, "Traduce cantidades a expresiones numericas"))
            .thenReturn(Optional.empty());
        when(capacidadRepository.save(any(Capacidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcion(2, 4, "Resuelve situaciones de adicion y sustraccion"))
            .thenReturn(Optional.empty());
        when(desempenoRepository.save(any(Desempeno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoPoblacionCurricular resultado = poblarCurriculoDesdeDocumentoService.execute("%PDF".getBytes(), null, null);

        assertEquals("Matematica", resultado.getArea().getNombre());
        assertEquals("1ro", resultado.getGrado().getNombre());
        verify(nivelEducativoRepository).save(any(NivelEducativo.class));
        verify(gradoRepository).save(any(Grado.class));
        verify(areaRepository).save(any(Area.class));
        verify(competenciaRepository).save(any(Competencia.class));
        verify(capacidadRepository).save(any(Capacidad.class));
        verify(desempenoRepository).save(any(Desempeno.class));
    }

    private CurriculoDocumentoParseResponse buildResponse() {
        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();

        CurriculoDocumentoParseResponse.CompetenciaItem competencia = new CurriculoDocumentoParseResponse.CompetenciaItem();
        competencia.setDescripcion("Resuelve problemas de cantidad");
        competencia.setCapacidades(Collections.singletonList("Traduce cantidades a expresiones numericas"));
        competencia.setDesempenos(Collections.singletonList("Resuelve situaciones de adicion y sustraccion"));

        CurriculoDocumentoParseResponse.AreaItem area = new CurriculoDocumentoParseResponse.AreaItem();
        area.setNombre("Matematica");
        area.setCompetencias(Collections.singletonList(competencia));

        CurriculoDocumentoParseResponse.GradoItem grado = new CurriculoDocumentoParseResponse.GradoItem();
        grado.setNombre("1ro");
        grado.setAreas(Collections.singletonList(area));

        CurriculoDocumentoParseResponse.NivelItem nivel = new CurriculoDocumentoParseResponse.NivelItem();
        nivel.setNombre("Primaria");
        nivel.setGrados(Collections.singletonList(grado));

        response.setNiveles(Collections.singletonList(nivel));
        return response;
    }
}
