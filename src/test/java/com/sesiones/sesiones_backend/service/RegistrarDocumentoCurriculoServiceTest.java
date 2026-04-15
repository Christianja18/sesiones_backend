package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.sesiones.sesiones_backend.dto.DocumentoCurriculoResponse;
import com.sesiones.sesiones_backend.dto.UploadDocumentoCurriculoRequest;
import com.sesiones.sesiones_backend.entity.Area;
import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;
import com.sesiones.sesiones_backend.entity.Grado;
import com.sesiones.sesiones_backend.mapper.SessionResponseMapper;
import com.sesiones.sesiones_backend.repository.DocumentoCurriculoRepository;

@ExtendWith(MockitoExtension.class)
class RegistrarDocumentoCurriculoServiceTest {

    @Mock
    private DocumentoCurriculoRepository documentoCurriculoRepository;

    @Mock
    private PoblarCurriculoDesdeDocumentoService poblarCurriculoDesdeDocumentoService;

    @Mock
    private SessionResponseMapper sessionResponseMapper;

    @InjectMocks
    private RegistrarDocumentoCurriculoService registrarDocumentoCurriculoService;

    @Test
    void shouldPersistPdfInDatabase() {
        MockMultipartFile archivo = new MockMultipartFile(
            "archivo",
            "curriculo.pdf",
            "application/pdf",
            "contenido-pdf".getBytes()
        );

        UploadDocumentoCurriculoRequest request = UploadDocumentoCurriculoRequest.builder()
            .archivo(archivo)
            .areaId(1)
            .gradoId(2)
            .build();

        Area area = new Area();
        area.setId(1);
        area.setNombre("Matematica");

        Grado grado = new Grado();
        grado.setId(2);
        grado.setNombre("4to");

        when(poblarCurriculoDesdeDocumentoService.execute(any(byte[].class), any(Integer.class), any(Integer.class)))
            .thenReturn(new PoblarCurriculoDesdeDocumentoService.ResultadoPoblacion(area, grado));
        when(documentoCurriculoRepository.save(any(DocumentoCurriculo.class))).thenAnswer(invocation -> {
            DocumentoCurriculo documentoCurriculo = invocation.getArgument(0);
            documentoCurriculo.setId(9);
            return documentoCurriculo;
        });
        when(sessionResponseMapper.toDocumentoCurriculoResponse(any(DocumentoCurriculo.class)))
            .thenReturn(DocumentoCurriculoResponse.builder().id(9).nombreArchivo("curriculo.pdf").build());

        DocumentoCurriculoResponse response = registrarDocumentoCurriculoService.execute(request);

        assertEquals(9, response.getId());
        assertEquals("curriculo.pdf", response.getNombreArchivo());

        ArgumentCaptor<DocumentoCurriculo> captor = ArgumentCaptor.forClass(DocumentoCurriculo.class);
        verify(documentoCurriculoRepository).save(captor.capture());
        assertEquals("curriculo.pdf", captor.getValue().getNombreArchivo());
        assertEquals(area, captor.getValue().getArea());
        assertEquals(grado, captor.getValue().getGrado());
        assertEquals("contenido-pdf", new String(captor.getValue().getArchivoPdf()));
    }
}
