package com.sesiones.sesiones_backend.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sesiones.sesiones_backend.entity.Docente;

public record DocentePrincipal(
    Integer id,
    String nombre,
    String email,
    String rol
) {

    public static DocentePrincipal from(Docente docente) {
        return new DocentePrincipal(
            docente.getId(),
            docente.getNombre(),
            docente.getEmail(),
            docente.getRol().getNombre()
        );
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
