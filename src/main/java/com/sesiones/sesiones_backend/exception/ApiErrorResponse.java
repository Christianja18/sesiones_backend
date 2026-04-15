package com.sesiones.sesiones_backend.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private List<String> details = new ArrayList<>();

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
