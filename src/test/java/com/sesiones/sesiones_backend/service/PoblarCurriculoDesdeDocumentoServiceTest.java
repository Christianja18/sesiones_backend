package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
import com.sesiones.sesiones_backend.entity.EstandarAprendizaje;
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
import com.sesiones.sesiones_backend.repository.EstandarAprendizajeRepository;
import com.sesiones.sesiones_backend.repository.GradoRepository;
import com.sesiones.sesiones_backend.repository.IngestLogRepository;
import com.sesiones.sesiones_backend.repository.NivelEducativoRepository;
import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;
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
    private EstandarAprendizajeRepository estandarAprendizajeRepository;

    @Mock
    private DesempenoRepository desempenoRepository;

    @Mock
    private IngestLogRepository ingestLogRepository;

    @InjectMocks
    private PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;

    @Test
    void shouldPopulateCompetenciasAndCapacidadesFromCurriculoDocument() {
        DocumentoCurriculo documento = buildDocumento(7, DocumentoCurriculoTipo.CURRICULO);
        Area area = buildArea();
        CurriculoDocumentoParseResponse response = buildCurriculoResponse();
        DocumentoChunkContenido chunk = buildChunk();

        when(documentoCurriculoRepository.findById(7)).thenReturn(Optional.of(documento));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> {
            DocumentoChunk saved = invocation.getArgument(0);
            saved.setId(11);
            return saved;
        });
        Competencia competencia = buildCompetencia(area);
        Capacidad capacidad = buildCapacidad(competencia);
        when(competenciaRepository.findByAreaIdAndDescripcionHash(3, hash("Resuelve problemas de cantidad")))
            .thenReturn(Optional.empty(), Optional.of(competencia));
        when(capacidadRepository.findByCompetenciaIdAndDescripcionHash(4, hash("Traduce cantidades a expresiones numericas")))
            .thenReturn(Optional.empty(), Optional.of(capacidad));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(7, "checksum-123", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(desempenoRepository, never()).save(any(Desempeno.class));
        verify(gradoRepository, never()).save(any(Grado.class));
        verify(competenciaRepository).upsertByAreaAndDescripcion(3, "Resuelve problemas de cantidad");
        verify(capacidadRepository).upsertByCompetenciaAndDescripcion(4, "Traduce cantidades a expresiones numericas");

        ArgumentCaptor<DocumentoChunkClasificacion> clasificacionCaptor = ArgumentCaptor.forClass(DocumentoChunkClasificacion.class);
        verify(documentoChunkClasificacionRepository).save(clasificacionCaptor.capture());
        assertEquals("Matematica", clasificacionCaptor.getValue().getArea().getNombre());

        ArgumentCaptor<DocumentoCurriculo> documentoCaptor = ArgumentCaptor.forClass(DocumentoCurriculo.class);
        verify(documentoCurriculoRepository).save(documentoCaptor.capture());
        assertEquals("checksum-123", documentoCaptor.getValue().getChecksumSha256());
        assertEquals(ProcesamientoEstado.PROCESADO, documentoCaptor.getValue().getEstado());
    }

    @Test
    void shouldPopulateDesempenosFromProgramaDocumentUsingExistingCompetencia() {
        DocumentoCurriculo documento = buildDocumento(8, DocumentoCurriculoTipo.PROGRAMA);
        NivelEducativo nivel = buildNivel();
        Ciclo ciclo = buildCiclo();
        Grado grado = buildGrado(nivel, ciclo);
        Area area = buildArea();
        Competencia competencia = buildCompetencia(area);
        CurriculoDocumentoParseResponse response = buildProgramaResponse();
        DocumentoChunkContenido chunk = buildChunk();

        when(documentoCurriculoRepository.findById(8)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(nivel));
        when(cicloRepository.findById("III")).thenReturn(Optional.of(ciclo));
        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(1, "1ro")).thenReturn(Optional.of(grado));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> {
            DocumentoChunk saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });
        when(competenciaRepository.findByAreaIdAndDescripcionHash(3, hash("Resuelve problemas de cantidad")))
            .thenReturn(Optional.of(competencia));
        Desempeno desempeno = buildDesempeno(grado, competencia, "Resuelve situaciones de adicion y sustraccion");
        when(desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcionHash(
            2,
            4,
            hash("Resuelve situaciones de adicion y sustraccion")
        )).thenReturn(Optional.empty(), Optional.of(desempeno));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(8, "checksum-456", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(competenciaRepository, never()).save(any(Competencia.class));
        verify(capacidadRepository, never()).save(any(Capacidad.class));
        verify(desempenoRepository).upsertByGradoCompetenciaAndDescripcion(
            2,
            4,
            "Resuelve situaciones de adicion y sustraccion",
            "oficial"
        );
        verify(desempenoRepository, never()).save(any(Desempeno.class));
    }

    @Test
    void shouldPopulateEstandaresFromCurriculoDocument() {
        DocumentoCurriculo documento = buildDocumento(10, DocumentoCurriculoTipo.CURRICULO);
        NivelEducativo nivel = buildNivel();
        Ciclo ciclo = buildCiclo();
        Area area = buildArea();
        Competencia competencia = buildCompetencia(area);
        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();
        CurriculoDocumentoParseResponse.Item item = new CurriculoDocumentoParseResponse.Item();
        item.setArea("Matematica");
        item.setNivel("Primaria");
        item.setCiclo("III");
        item.setCompetencia("Resuelve problemas de cantidad");
        item.setEstandares(List.of("Resuelve problemas referidos a acciones de juntar y separar cantidades"));
        response.setItems(List.of(item));
        DocumentoChunkContenido chunk = buildChunk();

        when(documentoCurriculoRepository.findById(10)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(nivel));
        when(cicloRepository.findById("III")).thenReturn(Optional.of(ciclo));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(competenciaRepository.findByAreaIdAndDescripcionHash(3, hash("Resuelve problemas de cantidad")))
            .thenReturn(Optional.of(competencia));
        when(estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdAndDescripcionHash(
            4,
            "III",
            hash("Resuelve problemas referidos a acciones de juntar y separar cantidades")
        )).thenReturn(Optional.empty(), Optional.of(buildEstandar(
            competencia,
            ciclo,
            "Resuelve problemas referidos a acciones de juntar y separar cantidades"
        )));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(10, "checksum-estandar", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(estandarAprendizajeRepository).upsertByCompetenciaCicloAndDescripcion(
            4,
            "III",
            "Resuelve problemas referidos a acciones de juntar y separar cantidades"
        );
    }

    @Test
    void shouldLinkProgramaDesempenoWithCapacidadAndEstandar() {
        DocumentoCurriculo documento = buildDocumento(11, DocumentoCurriculoTipo.PROGRAMA);
        NivelEducativo nivel = buildNivel();
        Ciclo ciclo = buildCiclo();
        Grado grado = buildGrado(nivel, ciclo);
        Area area = buildArea();
        Competencia competencia = buildCompetencia(area);
        Capacidad capacidad = buildCapacidad(competencia);
        EstandarAprendizaje estandar = buildEstandar(competencia, ciclo);
        CurriculoDocumentoParseResponse response = buildProgramaResponse();
        response.getItems().get(0).setCapacidades(List.of("Traduce cantidades a expresiones numericas"));
        response.getItems().get(0).setEstandares(List.of("Estandar del ciclo III"));
        DocumentoChunkContenido chunk = buildChunk();

        when(documentoCurriculoRepository.findById(11)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(nivel));
        when(cicloRepository.findById("III")).thenReturn(Optional.of(ciclo));
        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(1, "1ro")).thenReturn(Optional.of(grado));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(competenciaRepository.findByAreaIdAndDescripcionHash(3, hash("Resuelve problemas de cantidad")))
            .thenReturn(Optional.of(competencia));
        when(capacidadRepository.findByCompetenciaIdAndDescripcionHash(4, hash("Traduce cantidades a expresiones numericas")))
            .thenReturn(Optional.of(capacidad));
        when(estandarAprendizajeRepository.findByCompetenciaIdAndCicloIdAndDescripcionHash(4, "III", hash("Estandar del ciclo III")))
            .thenReturn(Optional.of(estandar));
        Desempeno desempeno = buildDesempeno(grado, competencia, "Resuelve situaciones de adicion y sustraccion");
        when(desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcionHash(
            2,
            4,
            hash("Resuelve situaciones de adicion y sustraccion")
        )).thenReturn(Optional.empty(), Optional.of(desempeno));
        when(desempenoRepository.save(any(Desempeno.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(11, "checksum-relaciones", List.of(new DocumentoChunkAnalizado(chunk, response)));

        ArgumentCaptor<Desempeno> desempenoCaptor = ArgumentCaptor.forClass(Desempeno.class);
        verify(desempenoRepository).save(desempenoCaptor.capture());
        Desempeno linkedDesempeno = desempenoCaptor.getValue();
        assertEquals(1, linkedDesempeno.getCapacidades().size());
        assertEquals(1, linkedDesempeno.getEstandares().size());
        assertEquals("Traduce cantidades a expresiones numericas", linkedDesempeno.getCapacidades().get(0).getDescripcion());
        assertEquals("Estandar del ciclo III", linkedDesempeno.getEstandares().get(0).getDescripcion());
    }

    @Test
    void shouldPopulateProgramaWhenCompetenciaCatalogDoesNotExist() {
        DocumentoCurriculo documento = buildDocumento(9, DocumentoCurriculoTipo.PROGRAMA);
        NivelEducativo nivel = buildNivel();
        Ciclo ciclo = buildCiclo();
        Grado grado = buildGrado(nivel, ciclo);
        Area area = buildArea();
        CurriculoDocumentoParseResponse response = buildProgramaResponse();
        DocumentoChunkContenido chunk = buildChunk();

        when(documentoCurriculoRepository.findById(9)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(nivel));
        when(cicloRepository.findById("III")).thenReturn(Optional.of(ciclo));
        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(1, "1ro")).thenReturn(Optional.of(grado));
        when(areaRepository.findAll()).thenReturn(List.of(area));
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Competencia competencia = buildCompetencia(area);
        Desempeno desempeno = buildDesempeno(grado, competencia, "Resuelve situaciones de adicion y sustraccion");
        when(competenciaRepository.findByAreaIdAndDescripcionHash(3, hash("Resuelve problemas de cantidad")))
            .thenReturn(Optional.empty(), Optional.of(competencia));
        when(desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcionHash(
            2,
            4,
            hash("Resuelve situaciones de adicion y sustraccion")
        )).thenReturn(Optional.empty(), Optional.of(desempeno));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(9, "checksum-789", List.of(new DocumentoChunkAnalizado(chunk, response)));

        verify(competenciaRepository).upsertByAreaAndDescripcion(3, "Resuelve problemas de cantidad");
        verify(desempenoRepository).upsertByGradoCompetenciaAndDescripcion(
            2,
            4,
            "Resuelve situaciones de adicion y sustraccion",
            "oficial"
        );
    }

    @Test
    void shouldPopulateProgramaCoreTablesWhenCatalogDataDoesNotExist() {
        DocumentoCurriculo documento = buildDocumento(12, DocumentoCurriculoTipo.PROGRAMA);
        NivelEducativo nivel = buildNivel();
        Ciclo ciclo = buildCiclo();
        Area area = buildArea();
        Grado grado = buildGrado(nivel, ciclo);
        Competencia competencia = buildCompetencia(area);
        Capacidad capacidad = buildCapacidad(competencia);
        Desempeno desempeno = buildDesempeno(grado, competencia, "Resuelve situaciones de adicion y sustraccion");
        CurriculoDocumentoParseResponse response = buildProgramaResponse();
        response.getItems().get(0).setCapacidades(List.of("Traduce cantidades a expresiones numericas"));
        DocumentoChunkContenido chunk = buildChunk();

        when(documentoCurriculoRepository.findById(12)).thenReturn(Optional.of(documento));
        when(nivelEducativoRepository.findAll()).thenReturn(List.of(nivel));
        when(cicloRepository.findById("III")).thenReturn(Optional.of(ciclo));
        when(gradoRepository.findByNivelIdAndNombreIgnoreCase(1, "1ro")).thenReturn(Optional.empty());
        when(gradoRepository.save(any(Grado.class))).thenAnswer(invocation -> {
            Grado saved = invocation.getArgument(0);
            saved.setId(2);
            return saved;
        });
        when(areaRepository.findAll()).thenReturn(List.of());
        when(areaRepository.save(any(Area.class))).thenAnswer(invocation -> {
            Area saved = invocation.getArgument(0);
            saved.setId(3);
            return saved;
        });
        when(documentoChunkRepository.save(any(DocumentoChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(competenciaRepository.findByAreaIdAndDescripcionHash(3, hash("Resuelve problemas de cantidad")))
            .thenReturn(Optional.empty(), Optional.of(competencia));
        when(capacidadRepository.findByCompetenciaIdAndDescripcionHash(4, hash("Traduce cantidades a expresiones numericas")))
            .thenReturn(Optional.empty(), Optional.of(capacidad));
        when(desempenoRepository.findByGradoIdAndCompetenciaIdAndDescripcionHash(
            2,
            4,
            hash("Resuelve situaciones de adicion y sustraccion")
        )).thenReturn(Optional.empty(), Optional.of(desempeno));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        poblarCurriculoDesdeDocumentoService.execute(12, "checksum-programa-full", List.of(new DocumentoChunkAnalizado(chunk, response)));

        ArgumentCaptor<Area> areaCaptor = ArgumentCaptor.forClass(Area.class);
        verify(areaRepository).save(areaCaptor.capture());
        assertEquals("Matematica", areaCaptor.getValue().getNombre());

        ArgumentCaptor<Grado> gradoCaptor = ArgumentCaptor.forClass(Grado.class);
        verify(gradoRepository).save(gradoCaptor.capture());
        assertEquals("1ro", gradoCaptor.getValue().getNombre());
        assertEquals("III", gradoCaptor.getValue().getCiclo().getId());

        verify(competenciaRepository).upsertByAreaAndDescripcion(3, "Resuelve problemas de cantidad");
        verify(capacidadRepository).upsertByCompetenciaAndDescripcion(4, "Traduce cantidades a expresiones numericas");
        verify(desempenoRepository).upsertByGradoCompetenciaAndDescripcion(
            2,
            4,
            "Resuelve situaciones de adicion y sustraccion",
            "oficial"
        );
    }

    private DocumentoCurriculo buildDocumento(Integer id, DocumentoCurriculoTipo tipo) {
        DocumentoCurriculo documento = new DocumentoCurriculo();
        documento.setId(id);
        documento.setTipo(tipo);
        documento.setNombreArchivo("curriculo.pdf");
        documento.setArchivoUrl("https://example.com/curriculo.pdf");
        documento.setEstado(ProcesamientoEstado.PROCESANDO);
        return documento;
    }

    private CurriculoDocumentoParseResponse buildCurriculoResponse() {
        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();
        CurriculoDocumentoParseResponse.Item item = new CurriculoDocumentoParseResponse.Item();
        item.setArea("Matematica");
        item.setCompetencia("Resuelve problemas de cantidad");
        item.setCapacidades(List.of("Traduce cantidades a expresiones numericas"));
        item.setDesempenos(List.of("No debe persistirse desde curriculo nacional"));
        item.setConfianza(new BigDecimal("0.92"));
        response.setItems(List.of(item));
        return response;
    }

    private CurriculoDocumentoParseResponse buildProgramaResponse() {
        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();
        CurriculoDocumentoParseResponse.Item item = new CurriculoDocumentoParseResponse.Item();
        item.setArea("Matematica");
        item.setNivel("Primaria");
        item.setGrado("1ro");
        item.setCiclo("III");
        item.setCompetencia("Resuelve problemas de cantidad");
        item.setDesempenos(List.of("Resuelve situaciones de adicion y sustraccion"));
        item.setConfianza(new BigDecimal("0.88"));
        response.setItems(List.of(item));
        return response;
    }

    private DocumentoChunkContenido buildChunk() {
        return new DocumentoChunkContenido(1, 1, 1, "contenido del chunk", "hash-chunk");
    }

    private NivelEducativo buildNivel() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(1);
        nivel.setNombre("Primaria");
        return nivel;
    }

    private Ciclo buildCiclo() {
        Ciclo ciclo = new Ciclo();
        ciclo.setId("III");
        ciclo.setNombre("Ciclo III");
        return ciclo;
    }

    private Grado buildGrado(NivelEducativo nivel, Ciclo ciclo) {
        Grado grado = new Grado();
        grado.setId(2);
        grado.setNombre("1ro");
        grado.setNivel(nivel);
        grado.setCiclo(ciclo);
        return grado;
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

    private EstandarAprendizaje buildEstandar(Competencia competencia, Ciclo ciclo) {
        return buildEstandar(competencia, ciclo, "Estandar del ciclo III");
    }

    private EstandarAprendizaje buildEstandar(Competencia competencia, Ciclo ciclo, String descripcion) {
        EstandarAprendizaje estandar = new EstandarAprendizaje();
        estandar.setId(6);
        estandar.setCompetencia(competencia);
        estandar.setCiclo(ciclo);
        estandar.setDescripcion(descripcion);
        return estandar;
    }

    private Desempeno buildDesempeno(Grado grado, Competencia competencia, String descripcion) {
        Desempeno desempeno = new Desempeno();
        desempeno.setId(7);
        desempeno.setGrado(grado);
        desempeno.setCompetencia(competencia);
        desempeno.setDescripcion(descripcion);
        return desempeno;
    }

    private String hash(String value) {
        String normalized = value.trim().replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
