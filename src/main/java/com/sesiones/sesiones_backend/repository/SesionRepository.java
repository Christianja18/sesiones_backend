package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Sesion;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    @EntityGraph(attributePaths = {
        "unidad",
        "unidad.grado",
        "unidad.grado.nivel",
        "unidad.area",
        "competencias",
        "capacidades",
        "desempenos",
        "actividades",
        "criteriosEvaluacion",
        "evidencias",
        "instrumentosEvaluacion"
    })
    Optional<Sesion> findDetailedById(Long id);
}
