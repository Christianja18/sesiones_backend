package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "rol")
    Optional<Docente> findByEmailIgnoreCase(String email);

    List<Docente> findAllByOrderByNombreAsc();
}

