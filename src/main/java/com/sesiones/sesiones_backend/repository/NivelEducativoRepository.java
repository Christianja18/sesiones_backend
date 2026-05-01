package com.sesiones.sesiones_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.NivelEducativo;

public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Integer> {
}

