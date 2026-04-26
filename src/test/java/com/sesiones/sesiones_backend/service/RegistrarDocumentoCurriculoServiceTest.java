package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.RegisterDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;
import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

@ExtendWith(MockitoExtension.class)
class RegistrarDocumentoCurriculoServiceTest {

    @Mock
    private DocumentoCurriculoRepository documentoCurriculoRepository;

    @Mock
    private SessionResponseMapper sessionResponseMapper;

    @InjectMocks
    private RegistrarDocumentoCurriculoService registrarDocumentoCurriculoService;

    @Test
    void shouldRejectNonPdfFileNames() {
        RegisterDocumentoCurriculoRequest request = RegisterDocumentoCurriculoRequest.builder()
            .tipo("curriculo")
            .nombreArchivo("curriculo.txt")
            .archivoUrl("https://minedu.gob.pe/curriculo/curriculo.txt")
            .build();

        assertThrows(BusinessRuleException.class, () -> registrarDocumentoCurriculoService.execute(request));
        verify(documentoCurriculoRepository, never()).save(any(DocumentoCurriculo.class));
    }

    @Test
    void shouldRejectDuplicatedDocumentUrl() {
        RegisterDocumentoCurriculoRequest request = RegisterDocumentoCurriculoRequest.builder()
            .tipo("curriculo")
            .nombreArchivo("curriculo.pdf")
            .archivoUrl("https://minedu.gob.pe/curriculo/curriculo.pdf")
            .build();

        when(documentoCurriculoRepository.existsByArchivoUrl("https://minedu.gob.pe/curriculo/curriculo.pdf")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> registrarDocumentoCurriculoService.execute(request));
        verify(documentoCurriculoRepository, never()).save(any(DocumentoCurriculo.class));
    }

    @Test
    void shouldRegisterDocumentReference() {
        RegisterDocumentoCurriculoRequest request = RegisterDocumentoCurriculoRequest.builder()
            .tipo("curriculo")
            .nombreArchivo("curriculo.pdf")
            .archivoUrl("https://minedu.gob.pe/curriculo/curriculo.pdf")
            .build();

        when(documentoCurriculoRepository.existsByArchivoUrl("https://minedu.gob.pe/curriculo/curriculo.pdf")).thenReturn(false);
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> {
            DocumentoCurriculo documento = invocation.getArgument(0);
            documento.setId(7);
            return documento;
        });
        when(sessionResponseMapper.toDocumentoCurriculoResponse(any(DocumentoCurriculo.class))).thenReturn(
            DocumentoCurriculoResponse.builder()
                .id(7)
                .tipo("curriculo")
                .nombreArchivo("curriculo.pdf")
                .archivoUrl("https://minedu.gob.pe/curriculo/curriculo.pdf")
                .estado("PENDIENTE")
                .build()
        );

        DocumentoCurriculoResponse response = registrarDocumentoCurriculoService.execute(request);

        ArgumentCaptor<DocumentoCurriculo> captor = ArgumentCaptor.forClass(DocumentoCurriculo.class);
        verify(documentoCurriculoRepository).save(captor.capture());

        DocumentoCurriculo savedDocumento = captor.getValue();
        assertEquals(com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo.CURRICULO, savedDocumento.getTipo());
        assertEquals("curriculo.pdf", savedDocumento.getNombreArchivo());
        assertEquals("https://minedu.gob.pe/curriculo/curriculo.pdf", savedDocumento.getArchivoUrl());
        assertEquals(ProcesamientoEstado.PENDIENTE, savedDocumento.getEstado());
        assertEquals(7, response.getId());
    }
}
