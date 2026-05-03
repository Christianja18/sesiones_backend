package com.sesiones.sesiones_backend.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sesiones.sesiones_backend.entity.Docente;
import com.sesiones.sesiones_backend.repository.DocenteRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;
    private final DocenteRepository docenteRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> token = resolveToken(request);
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = jwtService.validate(token.get());
            Docente docente = docenteRepository.findByEmailIgnoreCase(claims.email())
                .filter(found -> found.isActivo() && found.getId().equals(claims.userId()) && found.getRol() != null)
                .orElseThrow(() -> new JwtTokenException("Docente del token no valido"));
            DocentePrincipal principal = DocentePrincipal.from(docente);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.authorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtTokenException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new BadCredentialsException("Token invalido"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.of(authorizationHeader.substring(BEARER_PREFIX.length()));
        }
        return jwtCookieService.resolveToken(request);
    }
}
