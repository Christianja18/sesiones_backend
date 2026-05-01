package com.sesiones.sesiones_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Area;

public interface AreaRepository extends JpaRepository<Area, Integer> {

    List<Area> findAllByOrderByNombreAsc();
}

