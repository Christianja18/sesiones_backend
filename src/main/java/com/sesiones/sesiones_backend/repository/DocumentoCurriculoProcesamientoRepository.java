package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.DocumentoCurriculoProcesamiento;

public interface DocumentoCurriculoProcesamientoRepository extends JpaRepository<DocumentoCurriculoProcesamiento, Integer> {

    Optional<DocumentoCurriculoProcesamiento> findByDocumentoCurriculoId(Integer documentoCurriculoId);
}

