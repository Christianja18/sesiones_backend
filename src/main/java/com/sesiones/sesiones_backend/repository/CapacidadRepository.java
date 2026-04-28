package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sesiones.sesiones_backend.entity.Capacidad;

public interface CapacidadRepository extends JpaRepository<Capacidad, Integer> {

    List<Capacidad> findByCompetenciaIdOrderByIdAsc(Integer competenciaId);

    Optional<Capacidad> findByCompetenciaIdAndDescripcionHash(Integer competenciaId, String descripcionHash);

    @Modifying
    @Query(
        value = """
            INSERT INTO capacidad (competencia_id, descripcion)
            VALUES (:competenciaId, :descripcion)
            ON DUPLICATE KEY UPDATE id = id
            """,
        nativeQuery = true
    )
    int upsertByCompetenciaAndDescripcion(
        @Param("competenciaId") Integer competenciaId,
        @Param("descripcion") String descripcion
    );

    List<Capacidad> findByIdIn(List<Integer> ids);
}

