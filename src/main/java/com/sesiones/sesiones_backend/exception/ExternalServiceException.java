package com.sesiones.sesiones_backend.exception;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> details;

    public ExternalServiceException(String message) {
        this(HttpStatus.BAD_GATEWAY, message, Collections.emptyList(), null);
    }

    public ExternalServiceException(String message, Throwable cause) {
        this(HttpStatus.BAD_GATEWAY, message, Collections.emptyList(), cause);
    }

    public ExternalServiceException(HttpStatus status, String message, Throwable cause) {
        this(status, message, Collections.emptyList(), cause);
    }

    public ExternalServiceException(HttpStatus status, String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.details = details == null ? Collections.emptyList() : details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }
}
