package com.sesiones.sesiones_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Grado;

public interface GradoRepository extends JpaRepository<Grado, Long> {

    Optional<Grado> findByIdAndNivelId(Long id, Long nivelId);
}
