package com.sesiones.sesiones_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nivel_educativo")
public class NivelEducativo extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nombre;
}
