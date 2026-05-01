package com.sesiones.sesiones_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.DocumentoChunk;

public interface DocumentoChunkRepository extends JpaRepository<DocumentoChunk, Integer> {

    void deleteByDocumentoId(Integer documentoId);
}
