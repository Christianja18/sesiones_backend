package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.ActividadTipo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ActividadTipoConverter implements AttributeConverter<ActividadTipo, String> {

    @Override
    public String convertToDatabaseColumn(ActividadTipo attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public ActividadTipo convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ActividadTipo.fromDatabaseValue(dbData);
    }
}
