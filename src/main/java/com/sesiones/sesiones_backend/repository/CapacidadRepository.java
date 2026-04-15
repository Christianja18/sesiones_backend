package com.sesiones.sesiones_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Capacidad;

public interface CapacidadRepository extends JpaRepository<Capacidad, Integer> {

    List<Capacidad> findByCompetenciaIdOrderByIdAsc(Integer competenciaId);

    List<Capacidad> findByIdIn(List<Integer> ids);
}

