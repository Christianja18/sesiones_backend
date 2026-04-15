package com.sesiones.sesiones_backend.util.enums;

public enum DocumentoProcesamientoEstado {
    PENDIENTE("pendiente"),
    EN_PROCESO("en_proceso"),
    PROCESADO("procesado"),
    ERROR("error");

    private final String databaseValue;

    DocumentoProcesamientoEstado(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static DocumentoProcesamientoEstado fromDatabaseValue(String value) {
        for (DocumentoProcesamientoEstado estado : values()) {
            if (estado.databaseValue.equalsIgnoreCase(value)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado invalido");
    }
}
