package com.sesiones.sesiones_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sesiones.sesiones_backend.config.SecurityProperties;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtCookieServiceTest {

    @Mock
    private JwtService jwtService;

    @Test
    void shouldWriteHttpOnlyJwtCookie() {
        SecurityProperties securityProperties = new SecurityProperties();
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);
        JwtCookieService service = new JwtCookieService(securityProperties, jwtService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.addTokenCookie(response, "jwt-token");

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.contains("SESSION_TOKEN=jwt-token"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Max-Age=7200"));
    }

    @Test
    void shouldResolveJwtCookie() {
        SecurityProperties securityProperties = new SecurityProperties();
        JwtCookieService service = new JwtCookieService(securityProperties, jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("SESSION_TOKEN", "jwt-token"));

        assertEquals("jwt-token", service.resolveToken(request).orElseThrow());
    }
}
