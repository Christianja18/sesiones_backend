package com.sesiones.sesiones_backend.util.enums;

public enum DesempenoFuente {
    OFICIAL("oficial"),
    IA("ia");

    private final String databaseValue;

    DesempenoFuente(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static DesempenoFuente fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fuente de desempeno invalida");
        }

        String normalized = value.trim();
        for (DesempenoFuente fuente : values()) {
            if (fuente.databaseValue.equalsIgnoreCase(normalized) || fuente.name().equalsIgnoreCase(normalized)) {
                return fuente;
            }
        }
        throw new IllegalArgumentException("Fuente de desempeno invalida");
    }
}
