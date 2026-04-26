package com.sesiones.sesiones_backend.service;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;
import com.sesiones.sesiones_backend.util.enums.DocumentoCurriculoTipo;

public interface CurriculoLlmClient {

    CurriculoDocumentoParseResponse extraerCurriculo(String contenidoChunk, DocumentoCurriculoTipo tipoDocumento);
}
