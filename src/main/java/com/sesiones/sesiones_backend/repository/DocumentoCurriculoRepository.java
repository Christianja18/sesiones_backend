package com.sesiones.sesiones_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;

public interface DocumentoCurriculoRepository extends JpaRepository<DocumentoCurriculo, Integer> {

    boolean existsByArchivoUrl(String archivoUrl);

    boolean existsByChecksumSha256AndIdNot(String checksumSha256, Integer id);
}
