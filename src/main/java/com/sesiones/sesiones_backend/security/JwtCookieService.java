package com.sesiones.sesiones_backend.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sesiones.sesiones_backend.config.SecurityProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtCookieService {

    private final SecurityProperties securityProperties;
    private final JwtService jwtService;

    public void addTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, Duration.ofSeconds(jwtService.getExpirationSeconds())).toString());
    }

    public void clearTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public Optional<String> resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String cookieName = securityProperties.getCookie().getName();
        return Arrays.stream(cookies)
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(StringUtils::hasText)
            .findFirst();
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        SecurityProperties.Cookie properties = securityProperties.getCookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getName(), value)
            .httpOnly(true)
            .secure(properties.isSecure())
            .sameSite(properties.getSameSite())
            .path(properties.getPath())
            .maxAge(maxAge);

        if (StringUtils.hasText(properties.getDomain())) {
            builder.domain(properties.getDomain().trim());
        }
        return builder.build();
    }
}
