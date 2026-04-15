package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, Integer> {

    Optional<Institucion> findByNombreIgnoreCase(String nombre);
}

