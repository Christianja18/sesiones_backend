package com.sesiones.sesiones_backend.util.enums;

public enum ProcesamientoEstado {
    PENDIENTE("PENDIENTE"),
    PROCESANDO("PROCESANDO"),
    PROCESADO("PROCESADO"),
    ERROR("ERROR");

    private final String databaseValue;

    ProcesamientoEstado(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static ProcesamientoEstado fromDatabaseValue(String value) {
        for (ProcesamientoEstado estado : values()) {
            if (estado.databaseValue.equalsIgnoreCase(value)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de procesamiento invalido");
    }
}
