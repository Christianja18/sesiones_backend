package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.DocumentoCurriculo;

public interface DocumentoCurriculoRepository extends JpaRepository<DocumentoCurriculo, Integer> {

    @EntityGraph(attributePaths = {"area", "grado", "grado.nivel"})
    Optional<DocumentoCurriculo> findDetailedById(Integer id);
}

