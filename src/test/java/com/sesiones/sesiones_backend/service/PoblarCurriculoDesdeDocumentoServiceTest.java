package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.Capacidad;
import com.sesiones.sesiones_backend.entity.Ciclo;
import com.sesiones.sesiones_backend.entity.Competencia;
import com.sesiones.sesiones_backend.entity.Desempeno;
import com.sesiones.sesiones_backend.entity.DocumentoChunk;
import com.sesiones.sesiones_backend.entity.DocumentoChunkClasificacion;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.entity.NivelEducativo;
import com.sesiones.sesiones_backend.repository.AreaRepository;
import com.sesiones.sesiones_backend.repository.CapacidadRepository;
import com.sesiones.sesiones_backend.repository.CicloRepository;
import com.sesiones.sesiones_backend.repository.CompetenciaRepository;
import com.sesiones.sesiones_backend.repository.DesempenoRepository;
import com.sesiones.sesiones_backend.repository.DocumentoChunkClasificacionRepository;
import com.sesiones.sesiones_backend.repository.DocumentoChunkRepository;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;
import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

@ExtendWith(MockitoExtension.class)
class PoblarCurriculoDesdeDocumentoServiceTest {

    @Mock
    private DocumentoCurriculoRepository documentoCurriculoRepository;

    @Mock
    private DocumentoChunkRepository documentoChunkRepository;

    @Mock
    private DocumentoChunkClasificacionRepository documentoChunkClasificacionRepository;

    @Mock
    private NivelEducativoRepository nivelEducativoRepository;

    @Mock
    private CicloRepository cicloRepository;

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
    void shouldPopulateCurriculumEntitiesFromChunkAnalysis() {
        DocumentoCurriculo documento = new DocumentoCurriculo();
        documento.setId(7);
        documento.setNombreArchivo("curriculo.pdf");
        documento.setArchivoUrl("https://example.com/curriculo.pdf");
        documento.setEstado(ProcesamientoEstado.PROCESANDO);

        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(1);
        nivel.setNombre("Primaria");

        Ciclo ciclo = new Ciclo();
        ciclo.setId("III");
        ciclo.setNombre("Ciclo III");

        Grado grado = new Grado();
        grado.setId(2);
        grado.setNombre("1ro");
        grado.setNivel(nivel);
        grado.setCiclo(ciclo);

        Area area = new Area();
        area.setId(3);
        area.setNombre("Matematica");

        CurriculoDocumentoParseResponse response = buildResponse();
        DocumentoChunkContenido chunk = new DocumentoChunkContenido(
            1,
            1,
            2,
            "contenido del chunk",
            "hash-chunk"
        );

        when(documentoCurriculoRepository.findById(7)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(nivel));
        when(cicloRepository.findById("III")).thenReturn(Optional.of(ciclo));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(1, "1ro")).thenReturn(Optional.of(grado));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> {
            DocumentoChunk saved = invocation.getArgument(0);
            saved.setId(11);
            return saved;
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
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(7, "checksum-123", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(gradoRepository, never()).save(any(Grado.class));

        ArgumentCaptor<DocumentoChunkClasificacion> clasificacionCaptor = ArgumentCaptor.forClass(DocumentoChunkClasificacion.class);
        verify(documentoChunkClasificacionRepository).save(clasificacionCaptor.capture());
        assertEquals("1ro", clasificacionCaptor.getValue().getGrado().getNombre());
        assertEquals("III", clasificacionCaptor.getValue().getCiclo().getId());
        assertEquals("Matematica", clasificacionCaptor.getValue().getArea().getNombre());

        ArgumentCaptor<DocumentoCurriculo> documentoCaptor = ArgumentCaptor.forClass(DocumentoCurriculo.class);
        verify(documentoCurriculoRepository).save(documentoCaptor.capture());
        assertEquals("checksum-123", documentoCaptor.getValue().getChecksumSha256());
        assertEquals(ProcesamientoEstado.PROCESADO, documentoCaptor.getValue().getEstado());
    }

    @Test
    void shouldNormalizeNivelToCanonicalValuesOnly() {
        DocumentoCurriculo documento = new DocumentoCurriculo();
        documento.setId(8);
        documento.setNombreArchivo("curriculo.pdf");
        documento.setArchivoUrl("https://example.com/curriculo.pdf");
        documento.setEstado(ProcesamientoEstado.PROCESANDO);

        NivelEducativo primaria = new NivelEducativo();
        primaria.setId(1);
        primaria.setNombre("Primaria");

        Area area = new Area();
        area.setId(3);
        area.setNombre("Matematica");

        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();

        CurriculoDocumentoParseResponse.Item itemCanonico = new CurriculoDocumentoParseResponse.Item();
        itemCanonico.setArea("Matematica");
        itemCanonico.setNivel("Educación Primaria");
        itemCanonico.setConfianza(new BigDecimal("0.81"));

        CurriculoDocumentoParseResponse.Item itemBasura = new CurriculoDocumentoParseResponse.Item();
        itemBasura.setArea("Matematica");
        itemBasura.setNivel("Nivel 1");
        itemBasura.setConfianza(new BigDecimal("0.20"));

        response.setItems(List.of(itemCanonico, itemBasura));

        DocumentoChunkContenido chunk = new DocumentoChunkContenido(
            1,
            1,
            1,
            "contenido del chunk",
            "hash-chunk"
        );

        when(documentoCurriculoRepository.findById(8)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(primaria));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> {
            DocumentoChunk saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(8, "checksum-456", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(nivelEducativoRepository, never()).save(any(NivelEducativo.class));
        verify(documentoChunkClasificacionRepository).save(argThat(clasificacion ->
            clasificacion.getNivel() != null && "Primaria".equals(clasificacion.getNivel().getNombre())
        ));
    }

    @Test
    void shouldNotCreateGradoWhenCatalogEntryDoesNotExist() {
        DocumentoCurriculo documento = new DocumentoCurriculo();
        documento.setId(9);
        documento.setNombreArchivo("curriculo.pdf");
        documento.setArchivoUrl("https://example.com/curriculo.pdf");
        documento.setEstado(ProcesamientoEstado.PROCESANDO);

        NivelEducativo secundaria = new NivelEducativo();
        secundaria.setId(2);
        secundaria.setNombre("Secundaria");

        Ciclo ciclo = new Ciclo();
        ciclo.setId("VI");
        ciclo.setNombre("Ciclo VI");

        Area area = new Area();
        area.setId(3);
        area.setNombre("Matematica");

        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();
        CurriculoDocumentoParseResponse.Item item = new CurriculoDocumentoParseResponse.Item();
        item.setArea("Matematica");
        item.setNivel("Secundaria");
        item.setGrado("6to");
        item.setCiclo("VI");
        item.setCompetencia("Resuelve problemas de cantidad");
        item.setDesempenos(List.of("Desempeno no catalogado"));
        item.setConfianza(new BigDecimal("0.71"));
        response.setItems(List.of(item));

        DocumentoChunkContenido chunk = new DocumentoChunkContenido(
            1,
            1,
            1,
            "contenido del chunk",
            "hash-chunk"
        );

        when(documentoCurriculoRepository.findById(9)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(secundaria));
        when(cicloRepository.findById("VI")).thenReturn(Optional.of(ciclo));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(2, "6to")).thenReturn(Optional.empty());
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> {
            DocumentoChunk saved = invocation.getArgument(0);
            saved.setId(13);
            return saved;
        });
        when(competenciaRepository.findByAreaIdAndDescripcion(3, "Resuelve problemas de cantidad")).thenReturn(Optional.empty());
        when(competenciaRepository.save(any(Competencia.class))).thenAnswer(invocation -> {
            Competencia competencia = invocation.getArgument(0);
            competencia.setId(5);
            return competencia;
        });
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(9, "checksum-789", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(gradoRepository, never()).save(any(Grado.class));
        verify(desempenoRepository, never()).save(any(Desempeno.class));
        verify(documentoChunkClasificacionRepository).save(argThat(clasificacion ->
            clasificacion.getGrado() == null
                && clasificacion.getNivel() != null
                && "Secundaria".equals(clasificacion.getNivel().getNombre())
                && clasificacion.getCiclo() != null
                && "VI".equals(clasificacion.getCiclo().getId())
        ));
    }

    private CurriculoDocumentoParseResponse buildResponse() {
        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();

        CurriculoDocumentoParseResponse.Item item = new CurriculoDocumentoParseResponse.Item();
        item.setArea("Matematica");
        item.setCompetencia("Resuelve problemas de cantidad");
        item.setCapacidades(List.of("Traduce cantidades a expresiones numericas"));
        item.setDesempenos(List.of("Resuelve situaciones de adicion y sustraccion"));
        item.setNivel("Primaria");
        item.setGrado("1ro");
        item.setCiclo("III");
        item.setConfianza(new BigDecimal("0.92"));

        response.setItems(List.of(item));
        return response;
    }
}
