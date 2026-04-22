package com.sesiones.sesiones_backend.service;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;

public interface CurriculoLlmClient {

    CurriculoDocumentoParseResponse extraerCurriculo(String textoPdf, String areaSugerida, String gradoSugerido, String nivelSugerido);
}
