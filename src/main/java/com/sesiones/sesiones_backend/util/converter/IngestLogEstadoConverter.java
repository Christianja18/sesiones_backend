package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.IngestLogEstado;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IngestLogEstadoConverter implements AttributeConverter<IngestLogEstado, String> {

    @Override
    public String convertToDatabaseColumn(IngestLogEstado attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public IngestLogEstado convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IngestLogEstado.fromDatabaseValue(dbData);
    }
}
