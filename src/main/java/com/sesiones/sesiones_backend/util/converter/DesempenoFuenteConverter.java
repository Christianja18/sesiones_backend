package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.DesempenoFuente;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DesempenoFuenteConverter implements AttributeConverter<DesempenoFuente, String> {

    @Override
    public String convertToDatabaseColumn(DesempenoFuente attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public DesempenoFuente convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DesempenoFuente.fromDatabaseValue(dbData);
    }
}
