package com.sesiones.sesiones_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sesiones.sesiones_backend.dto.LoginRequest;
import com.sesiones.sesiones_backend.dto.LoginResponse;
import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.entity.Rol;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldLoginWithValidCredentials() {
        LoginRequest request = request("PROFESOR@demo.edu.pe", "Docente123");
        Docente docente = docente();

        when(docenteRepository.findByEmailIgnoreCase("profesor@demo.edu.pe")).thenReturn(Optional.of(docente));
        when(passwordEncoder.matches("Docente123", "{bcrypt}hash")).thenReturn(true);
        when(jwtService.createToken(docente)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        LoginResponse response = authService.login(request, "127.0.0.1");

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("profesor@demo.edu.pe", response.getDocente().getEmail());
        assertEquals("PROFESOR", response.getDocente().getRol());
        verify(loginRateLimiter).reset("profesor@demo.edu.pe", "127.0.0.1");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = request("profesor@demo.edu.pe", "wrong-password");
        Docente docente = docente();

        when(docenteRepository.findByEmailIgnoreCase("profesor@demo.edu.pe")).thenReturn(Optional.of(docente));
        when(passwordEncoder.matches("wrong-password", "{bcrypt}hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request, "127.0.0.1"));
        verify(loginRateLimiter).recordFailure("profesor@demo.edu.pe", "127.0.0.1");
    }

    private LoginRequest request(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private Docente docente() {
        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("PROFESOR");

        Docente docente = new Docente();
        docente.setId(10);
        docente.setNombre("Profesor Demo");
        docente.setEmail("profesor@demo.edu.pe");
        docente.setRol(rol);
        docente.setPasswordHash("{bcrypt}hash");
        docente.setActivo(true);
        return docente;
    }
}
