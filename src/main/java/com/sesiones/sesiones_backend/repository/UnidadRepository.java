package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Unidad;

public interface UnidadRepository extends JpaRepository<Unidad, Long> {

    @EntityGraph(attributePaths = {"grado", "grado.nivel", "area", "docente", "docente.institucion"})
    Optional<Unidad> findDetailedById(Long id);
}
