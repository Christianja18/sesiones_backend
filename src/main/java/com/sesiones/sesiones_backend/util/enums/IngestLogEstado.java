package com.sesiones.sesiones_backend.util.enums;

public enum IngestLogEstado {
    OK("OK"),
    ERROR("ERROR");

    private final String databaseValue;

    IngestLogEstado(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static IngestLogEstado fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Estado de log de ingesta invalido");
        }

        for (IngestLogEstado estado : values()) {
            if (estado.databaseValue.equalsIgnoreCase(value.trim())) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de log de ingesta invalido");
    }
}
