package com.sesiones.sesiones_backend.util.enums;

public enum InstrumentoTipo {
    RUBRICA("rubrica"),
    LISTA_COTEJO("lista_cotejo");

    private final String databaseValue;

    InstrumentoTipo(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static InstrumentoTipo fromDatabaseValue(String value) {
        for (InstrumentoTipo tipo : values()) {
            if (tipo.databaseValue.equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de instrumento invalido");
    }
}
