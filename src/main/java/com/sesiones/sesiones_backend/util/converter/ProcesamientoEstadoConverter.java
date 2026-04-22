package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProcesamientoEstadoConverter implements AttributeConverter<ProcesamientoEstado, String> {

    @Override
    public String convertToDatabaseColumn(ProcesamientoEstado attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public ProcesamientoEstado convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProcesamientoEstado.fromDatabaseValue(dbData);
    }
}
