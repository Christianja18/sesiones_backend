package com.sesiones.sesiones_backend.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sesiones.sesiones_backend.dto.AuthUserResponse;
import com.sesiones.sesiones_backend.dto.LoginRequest;
import com.sesiones.sesiones_backend.dto.LoginResponse;
import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.repository.DocenteRepository;
import com.sesiones.sesiones_backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales invalidas";

    private final DocenteRepository docenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String email = request.getEmail().trim().toLowerCase();
        loginRateLimiter.assertAllowed(email, clientIp);

        Docente docente = docenteRepository.findByEmailIgnoreCase(email)
            .filter(this::canAuthenticate)
            .filter(found -> passwordMatches(request.getPassword(), found.getPasswordHash()))
            .orElseThrow(() -> {
                loginRateLimiter.recordFailure(email, clientIp);
                return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
            });

        loginRateLimiter.reset(email, clientIp);
        docente.setUltimoLoginAt(LocalDateTime.now());
        String token = jwtService.createToken(docente);
        return LoginResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .expiresInSeconds(jwtService.getExpirationSeconds())
            .docente(toUserResponse(docente))
            .build();
    }

    private boolean canAuthenticate(Docente docente) {
        return docente.isActivo()
            && docente.getRol() != null
            && docente.getPasswordHash() != null
            && !docente.getPasswordHash().isBlank();
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        try {
            return passwordHash != null && passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private AuthUserResponse toUserResponse(Docente docente) {
        return AuthUserResponse.builder()
            .id(docente.getId())
            .nombre(docente.getNombre())
            .email(docente.getEmail())
            .rol(docente.getRol().getNombre())
            .build();
    }
}
