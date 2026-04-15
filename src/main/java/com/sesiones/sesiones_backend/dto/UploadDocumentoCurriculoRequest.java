package com.sesiones.sesiones_backend.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentoCurriculoRequest {

    @NotNull
    private MultipartFile archivo;

    private Integer areaId;

    private Integer gradoId;
}
