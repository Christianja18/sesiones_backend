package com.sesiones.sesiones_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sesiones.sesiones_backend.dto.AuthUserResponse;
import com.sesiones.sesiones_backend.dto.CsrfTokenResponse;
import com.sesiones.sesiones_backend.dto.LoginRequest;
import com.sesiones.sesiones_backend.dto.LoginResponse;
import com.sesiones.sesiones_backend.security.DocentePrincipal;
import com.sesiones.sesiones_backend.security.JwtCookieService;
import com.sesiones.sesiones_backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Login y emision de JWT")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        LoginResponse response = authService.login(request, httpRequest.getRemoteAddr());
        jwtCookieService.addTokenCookie(httpResponse, response.getAccessToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener docente autenticado")
    public ResponseEntity<AuthUserResponse> me(@AuthenticationPrincipal DocentePrincipal principal) {
        return ResponseEntity.ok(AuthUserResponse.builder()
            .id(principal.id())
            .nombre(principal.nombre())
            .email(principal.email())
            .rol(principal.rol())
            .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        jwtCookieService.clearTokenCookie(response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtener token CSRF")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(CsrfTokenResponse.builder()
            .headerName(csrfToken.getHeaderName())
            .token(csrfToken.getToken())
            .build());
    }
}
