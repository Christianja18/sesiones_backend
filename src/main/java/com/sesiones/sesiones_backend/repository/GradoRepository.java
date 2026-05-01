package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sesiones.sesiones_backend.entity.Grado;

public interface GradoRepository extends JpaRepository<Grado, Integer> {

    Optional<Grado> findByIdAndNivelId(Integer id, Integer nivelId);

    Optional<Grado> findByNivelIdAndNombreIgnoreCase(Integer nivelId, String nombre);

    List<Grado> findByNivelIdOrderByIdAsc(Integer nivelId);

    @Query("""
        select g
        from Grado g
        join g.nivel n
        where (:nivelId is null or n.id = :nivelId)
        order by n.id asc, g.id asc
        """)
    List<Grado> findCatalog(@Param("nivelId") Integer nivelId);
}

