package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;

public interface DocumentoCurriculoRepository extends JpaRepository<DocumentoCurriculo, Long> {

    @EntityGraph(attributePaths = {"area", "grado", "grado.nivel", "procesamiento"})
    Optional<DocumentoCurriculo> findDetailedById(Long id);
}
