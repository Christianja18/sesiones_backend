package com.sesiones.sesiones_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
