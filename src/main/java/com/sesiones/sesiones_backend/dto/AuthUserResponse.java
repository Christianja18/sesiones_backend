package com.sesiones.sesiones_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthUserResponse {

    private Integer id;
    private String nombre;
    private String email;
    private String rol;
}
