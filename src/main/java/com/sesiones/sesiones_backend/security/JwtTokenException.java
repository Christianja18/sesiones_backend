package com.sesiones.sesiones_backend.security;

public class JwtTokenException extends RuntimeException {

    public JwtTokenException(String message) {
        super(message);
    }
}
