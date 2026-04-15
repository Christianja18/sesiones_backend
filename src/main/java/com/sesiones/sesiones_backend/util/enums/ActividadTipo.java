package com.sesiones.sesiones_backend.util.enums;

public enum ActividadTipo {
    INICIO("inicio"),
    DESARROLLO("desarrollo"),
    CIERRE("cierre");

    private final String databaseValue;

    ActividadTipo(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static ActividadTipo fromDatabaseValue(String value) {
        for (ActividadTipo tipo : values()) {
            if (tipo.databaseValue.equalsIgnoreCase(value)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de actividad invalido");
    }
}
