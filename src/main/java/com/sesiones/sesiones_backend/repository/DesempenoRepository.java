package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sesiones.sesiones_backend.entity.Desempeno;

public interface DesempenoRepository extends JpaRepository<Desempeno, Integer> {

    List<Desempeno> findByGradoIdAndCompetenciaIdOrderByIdAsc(Integer gradoId, Integer competenciaId);

    Optional<Desempeno> findByGradoIdAndCompetenciaIdAndDescripcionHash(
        Integer gradoId,
        Integer competenciaId,
        String descripcionHash
    );

    @Modifying
    @Query(
        value = """
            INSERT INTO desempeno (grado_id, competencia_id, descripcion, fuente)
            VALUES (:gradoId, :competenciaId, :descripcion, :fuente)
            ON DUPLICATE KEY UPDATE id = id
            """,
        nativeQuery = true
    )
    int upsertByGradoCompetenciaAndDescripcion(
        @Param("gradoId") Integer gradoId,
        @Param("competenciaId") Integer competenciaId,
        @Param("descripcion") String descripcion,
        @Param("fuente") String fuente
    );

    List<Desempeno> findByIdIn(List<Integer> ids);
}

