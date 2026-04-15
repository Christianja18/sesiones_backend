package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {

    Optional<Competencia> findByIdAndAreaId(Long id, Long areaId);

    List<Competencia> findByIdIn(List<Long> ids);
}
