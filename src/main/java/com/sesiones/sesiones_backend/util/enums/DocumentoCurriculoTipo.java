package com.sesiones.sesiones_backend.util.enums;

public enum DocumentoCurriculoTipo {
    CURRICULO("curriculo"),
    PROGRAMA("programa");

    private final String databaseValue;

    DocumentoCurriculoTipo(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static DocumentoCurriculoTipo fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tipo de documento curricular invalido");
        }

        String normalized = value.trim();
        for (DocumentoCurriculoTipo tipo : values()) {
            if (tipo.databaseValue.equalsIgnoreCase(normalized) || tipo.name().equalsIgnoreCase(normalized)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de documento curricular invalido");
    }
}
