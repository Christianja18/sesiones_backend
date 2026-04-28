package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sesiones.sesiones_backend.entity.EstandarAprendizaje;

public interface EstandarAprendizajeRepository extends JpaRepository<EstandarAprendizaje, Integer> {

    List<EstandarAprendizaje> findByCompetenciaIdAndCicloIdOrderByIdAsc(Integer competenciaId, String cicloId);

    Optional<EstandarAprendizaje> findByCompetenciaIdAndCicloIdAndDescripcionHash(
        Integer competenciaId,
        String cicloId,
        String descripcionHash
    );

    @Modifying
    @Query(
        value = """
            INSERT INTO estandar_aprendizaje (competencia_id, ciclo_id, descripcion)
            VALUES (:competenciaId, :cicloId, :descripcion)
            ON DUPLICATE KEY UPDATE id = id
            """,
        nativeQuery = true
    )
    int upsertByCompetenciaCicloAndDescripcion(
        @Param("competenciaId") Integer competenciaId,
        @Param("cicloId") String cicloId,
        @Param("descripcion") String descripcion
    );
}
