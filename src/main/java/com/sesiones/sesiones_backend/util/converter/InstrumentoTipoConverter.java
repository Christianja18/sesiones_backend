package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InstrumentoTipoConverter implements AttributeConverter<InstrumentoTipo, String> {

    @Override
    public String convertToDatabaseColumn(InstrumentoTipo attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public InstrumentoTipo convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InstrumentoTipo.fromDatabaseValue(dbData);
    }
}
