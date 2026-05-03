package com.sesiones.sesiones_backend.security;

import java.util.Arrays;
import java.util.Set;

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sesiones.sesiones_backend.config.SecurityProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CookieCsrfRequestMatcher implements RequestMatcher {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SecurityProperties securityProperties;

    @Override
    public boolean matches(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        String sessionCookieName = securityProperties.getCookie().getName();
        return Arrays.stream(cookies)
            .anyMatch(cookie -> sessionCookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue()));
    }
}
