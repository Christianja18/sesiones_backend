package com.sesiones.sesiones_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sesiones.sesiones_backend.entity.Grado;

public interface GradoRepository extends JpaRepository<Grado, Integer> {

    Optional<Grado> findByIdAndNivelId(Integer id, Integer nivelId);

    List<Grado> findByNombreIgnoreCase(String nombre);

    Optional<Grado> findByNombreIgnoreCaseAndNivelNombreIgnoreCase(String nombre, String nivelNombre);
}

