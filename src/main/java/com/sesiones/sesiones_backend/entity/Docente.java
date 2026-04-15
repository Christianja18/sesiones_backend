package com.sesiones.sesiones_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "docente")
public class Docente extends CreationAuditableEntity {

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;
}
