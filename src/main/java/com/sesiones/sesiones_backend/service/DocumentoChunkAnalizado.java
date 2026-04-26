package com.sesiones.sesiones_backend.service;

import com.sesiones.sesiones_backend.dto.CurriculoDocumentoParseResponse;

record DocumentoChunkAnalizado(
    DocumentoChunkContenido chunk,
    CurriculoDocumentoParseResponse analisis
) {
}
