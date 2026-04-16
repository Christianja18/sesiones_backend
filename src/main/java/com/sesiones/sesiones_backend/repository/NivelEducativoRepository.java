package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.NivelEducativo;

public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Integer> {

    List<NivelEducativo> findAllByOrderByNombreAsc();

    Optional<NivelEducativo> findByNombreIgnoreCase(String nombre);
}

