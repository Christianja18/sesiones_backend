package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.EstandarAprendizaje;

public interface EstandarAprendizajeRepository extends JpaRepository<EstandarAprendizaje, Integer> {

    List<EstandarAprendizaje> findByCompetenciaIdAndCicloIdOrderByIdAsc(Integer competenciaId, String cicloId);

    Optional<EstandarAprendizaje> findByCompetenciaIdAndCicloIdAndDescripcion(
        Integer competenciaId,
        String cicloId,
        String descripcion
    );
}
