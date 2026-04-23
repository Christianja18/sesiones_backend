package com.sesiones.sesiones_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.DocumentoChunk;

public interface DocumentoChunkRepository extends JpaRepository<DocumentoChunk, Integer> {

    void deleteByDocumentoId(Integer documentoId);

    List<DocumentoChunk> findByDocumentoIdOrderByOrdenAsc(Integer documentoId);
}
