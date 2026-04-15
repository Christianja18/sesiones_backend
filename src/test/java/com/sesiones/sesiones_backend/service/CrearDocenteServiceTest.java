package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sesiones.sesiones_backend.dto.CreateDocenteRequest;
import com.sesiones.sesiones_backend.dto.DocenteResponse;
import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.entity.Institucion;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.repository.InstitucionRepository;
import com.sesiones.sesiones_backend.exception.BusinessRuleException;

@ExtendWith(MockitoExtension.class)
class CrearDocenteServiceTest {

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private InstitucionRepository institucionRepository;

    @InjectMocks
    private CrearDocenteService crearDocenteService;

    @Test
    void shouldRejectDuplicatedEmail() {
        CreateDocenteRequest request = CreateDocenteRequest.builder()
            .nombre("Ana PÃ©rez")
            .email("ana@correo.com")
            .institucion("IE 001")
            .build();

        when(docenteRepository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> crearDocenteService.execute(request));
    }

    @Test
    void shouldCreateTeacher() {
        CreateDocenteRequest request = CreateDocenteRequest.builder()
            .nombre("Ana PÃ©rez")
            .email("ANA@correo.com")
            .institucion("IE 001")
            .build();

        when(docenteRepository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(false);
        when(institucionRepository.findByNombreIgnoreCase("IE 001")).thenReturn(java.util.Optional.empty());
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(invocation -> {
            Institucion institucion = invocation.getArgument(0);
            institucion.setId(3L);
            return institucion;
        });
        when(docenteRepository.save(any(Docente.class))).thenAnswer(invocation -> {
            Docente docente = invocation.getArgument(0);
            docente.setId(10L);
            return docente;
        });

        DocenteResponse response = crearDocenteService.execute(request);

        assertEquals(10L, response.getId());
        assertEquals("ana@correo.com", response.getEmail());
        assertEquals(3L, response.getInstitucionId());
        assertEquals("IE 001", response.getInstitucion());
        verify(docenteRepository).save(any(Docente.class));
    }
}

