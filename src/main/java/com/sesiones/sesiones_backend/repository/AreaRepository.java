package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Area;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    Optional<Area> findByNombreIgnoreCase(String nombre);
}

