package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DocumentoCurriculoTipoConverter implements AttributeConverter<DocumentoCurriculoTipo, String> {

    @Override
    public String convertToDatabaseColumn(DocumentoCurriculoTipo attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public DocumentoCurriculoTipo convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DocumentoCurriculoTipo.fromDatabaseValue(dbData);
    }
}
