package com.sesiones.sesiones_backend.util.converter;

import com.sesiones.sesiones_backend.util.enums.DocumentoProcesamientoEstado;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DocumentoProcesamientoEstadoConverter implements AttributeConverter<DocumentoProcesamientoEstado, String> {

    @Override
    public String convertToDatabaseColumn(DocumentoProcesamientoEstado attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public DocumentoProcesamientoEstado convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DocumentoProcesamientoEstado.fromDatabaseValue(dbData);
    }
}
