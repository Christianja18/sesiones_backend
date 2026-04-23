package com.sesiones.sesiones_backend.service;

record DocumentoChunkContenido(
    int orden,
    Integer paginaInicio,
    Integer paginaFin,
    String contenido,
    String hashContenido
) {
}
