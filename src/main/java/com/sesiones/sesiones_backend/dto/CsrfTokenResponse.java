package com.sesiones.sesiones_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CsrfTokenResponse {

    private String headerName;
    private String token;
}
