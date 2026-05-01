package com.sesiones.sesiones_backend.security;

import java.time.Instant;

public record JwtClaims(
    Integer userId,
    String email,
    String rol,
    Instant issuedAt,
    Instant expiresAt
) {
}
