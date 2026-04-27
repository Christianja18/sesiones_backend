package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.repository.IngestLogRepository;
import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;
import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

@ExtendWith(MockitoExtension.class)
class ProcesarDocumentoCurriculoServiceTest {

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private DocumentoCurriculoRepository documentoCurriculoRepository;

    @Mock
    private DocumentoCurriculoDownloaderService documentoCurriculoDownloaderService;

    @Mock
    private PdfTextExtractorService pdfTextExtractorService;

    @Mock
    private DocumentoCurriculoChunkerService documentoCurriculoChunkerService;

    @Mock
    private CurriculoLlmClient curriculoLlmClient;

    @Mock
    private PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;

    @Mock
    private SessionResponseMapper sessionResponseMapper;

    @Mock
    private IngestLogRepository ingestLogRepository;

    @InjectMocks
    private ProcesarDocumentoCurriculoService procesarDocumentoCurriculoService;

    @Test
    void shouldProcessDocumentFromUrlAndPersistIngestion() {
        DocumentoCurriculo documento = buildDocumento();
        byte[] pdf = "%PDF".getBytes();
        List<PaginaPdfTexto> pages = List.of(new PaginaPdfTexto(1, "texto pagina 1"));
        List<DocumentoChunkContenido> chunks = List.of(new DocumentoChunkContenido(1, 1, 1, "chunk 1", "hash-1"));
        CurriculoDocumentoParseResponse response = new CurriculoDocumentoParseResponse();
        response.setItems(List.of(new CurriculoDocumentoParseResponse.Item()));

        when(referenceResolver.findDocumentoCurriculo(7)).thenReturn(documento);
        when(documentoCurriculoDownloaderService.download(documento.getArchivoUrl())).thenReturn(pdf);
        when(documentoCurriculoRepository.existsByChecksumSha256AndIdNot(any(), any())).thenReturn(false);
        when(pdfTextExtractorService.extractPages(pdf)).thenReturn(pages);
        when(documentoCurriculoChunkerService.chunk(pages)).thenReturn(chunks);
        when(curriculoLlmClient.extraerCurriculo("chunk 1", DocumentoCurriculoTipo.CURRICULO)).thenReturn(response);
        when(sessionResponseMapper.toDocumentoCurriculoResponse(documento)).thenReturn(
            DocumentoCurriculoResponse.builder().id(7).estado("PROCESADO").build()
        );

        DocumentoCurriculoResponse result = procesarDocumentoCurriculoService.execute(7);

        assertEquals(7, result.getId());
        verify(poblarCurriculoDesdeDocumentoService).execute(any(), any(), any());
        verify(documentoCurriculoRepository, atLeastOnce()).save(any(DocumentoCurriculo.class));
    }

    @Test
    void shouldMarkDocumentAsErrorWhenProcessingFails() {
        DocumentoCurriculo documento = buildDocumento();

        when(referenceResolver.findDocumentoCurriculo(7)).thenReturn(documento);
        when(documentoCurriculoDownloaderService.download(documento.getArchivoUrl()))
            .thenThrow(new BusinessRuleException("Fallo de descarga"));

        assertThrows(BusinessRuleException.class, () -> procesarDocumentoCurriculoService.execute(7));

        ArgumentCaptor<DocumentoCurriculo> captor = ArgumentCaptor.forClass(DocumentoCurriculo.class);
        verify(documentoCurriculoRepository, atLeastOnce()).save(captor.capture());
        DocumentoCurriculo ultimoGuardado = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(ProcesamientoEstado.ERROR, ultimoGuardado.getEstado());
        assertEquals("Fallo de descarga", ultimoGuardado.getErrorDetalle());
    }

    private DocumentoCurriculo buildDocumento() {
        DocumentoCurriculo documento = new DocumentoCurriculo();
        documento.setId(7);
        documento.setTipo(DocumentoCurriculoTipo.CURRICULO);
        documento.setNombreArchivo("curriculo.pdf");
        documento.setArchivoUrl("https://example.com/curriculo.pdf");
        documento.setEstado(ProcesamientoEstado.PENDIENTE);
        return documento;
    }
}
