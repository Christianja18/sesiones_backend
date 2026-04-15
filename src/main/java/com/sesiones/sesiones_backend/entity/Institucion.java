package com.sesiones.sesiones_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "institucion")
public class Institucion extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String nombre;
}
