package com.sesiones.sesiones_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    @JsonIgnore
    private String accessToken;

    @JsonIgnore
    private String tokenType;

    private long expiresInSeconds;
    private AuthUserResponse docente;
}
