package com.sesiones.sesiones_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Ciclo;

public interface CicloRepository extends JpaRepository<Ciclo, String> {
}
