package com.sesiones.sesiones_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Desempeno;

public interface DesempenoRepository extends JpaRepository<Desempeno, Long> {

    List<Desempeno> findByGradoIdAndCompetenciaIdOrderByIdAsc(Long gradoId, Long competenciaId);

    List<Desempeno> findByIdIn(List<Long> ids);
}
