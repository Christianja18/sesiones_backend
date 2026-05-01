package com.sesiones.sesiones_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocenteRequest {

    @NotBlank
    private String nombre;

    @Email
    @NotBlank
    private String email;

    private String institucion;
}
