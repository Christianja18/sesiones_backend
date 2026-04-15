package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.UploadDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;

@ExtendWith(MockitoExtension.class)
class RegistrarDocumentoCurriculoServiceTest {

    @Mock
    private ReferenceResolver referenceResolver;

    @Mock
    private DocumentoCurriculoRepository documentoCurriculoRepository;

    @Mock
    private SessionResponseMapper sessionResponseMapper;

    @InjectMocks
    private RegistrarDocumentoCurriculoService registrarDocumentoCurriculoService;

    @Test
    void shouldRejectNonPdfFiles() {
        MockMultipartFile archivo = new MockMultipartFile(
            "archivo",
            "curriculo.txt",
            "text/plain",
            "contenido".getBytes()
        );

        UploadDocumentoCurriculoRequest request = UploadDocumentoCurriculoRequest.builder()
            .archivo(archivo)
            .build();

        assertThrows(BusinessRuleException.class, () -> registrarDocumentoCurriculoService.execute(request));
    }

    @Test
    void shouldRegisterPdfDocument() {
        byte[] contenido = "%PDF-1.4".getBytes();
        MockMultipartFile archivo = new MockMultipartFile(
            "archivo",
            "curriculo.pdf",
            "application/pdf",
            contenido
        );

        UploadDocumentoCurriculoRequest request = UploadDocumentoCurriculoRequest.builder()
            .archivo(archivo)
            .build();

        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> {
            DocumentoCurriculo documento = invocation.getArgument(0);
            documento.setId(7);
            return documento;
        });
        when(sessionResponseMapper.toDocumentoCurriculoResponse(any(DocumentoCurriculo.class))).thenReturn(
            DocumentoCurriculoResponse.builder()
                .id(7)
                .nombreArchivo("curriculo.pdf")
                .build()
        );

        DocumentoCurriculoResponse response = registrarDocumentoCurriculoService.execute(request);

        ArgumentCaptor<DocumentoCurriculo> captor = ArgumentCaptor.forClass(DocumentoCurriculo.class);
        verify(documentoCurriculoRepository).save(captor.capture());

        DocumentoCurriculo savedDocumento = captor.getValue();
        assertEquals("curriculo.pdf", savedDocumento.getNombreArchivo());
        assertEquals("%PDF-1.4", new String(savedDocumento.getArchivoPdf()));
        assertEquals(7, response.getId());
    }
}
