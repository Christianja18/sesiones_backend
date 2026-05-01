package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sesiones.sesiones_backend.entity.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, Integer> {

    Optional<Competencia> findByIdAndAreaId(Integer id, Integer areaId);

    Optional<Competencia> findByAreaIdAndDescripcionHash(Integer areaId, String descripcionHash);

    List<Competencia> findByAreaIdOrderByIdAsc(Integer areaId);

    @Query("""
        select c
        from Competencia c
        join c.area a
        where (:areaId is null or a.id = :areaId)
        order by a.nombre asc, c.id asc
        """)
    List<Competencia> findCatalog(@Param("areaId") Integer areaId);

    @Modifying
    @Query(
        value = """
            INSERT INTO competencia (area_id, descripcion)
            VALUES (:areaId, :descripcion)
            ON DUPLICATE KEY UPDATE id = id
            """,
        nativeQuery = true
    )
    int upsertByAreaAndDescripcion(
        @Param("areaId") Integer areaId,
        @Param("descripcion") String descripcion
    );

    List<Competencia> findByIdIn(List<Integer> ids);
}

