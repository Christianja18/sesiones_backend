package com.sesiones.sesiones_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ciclo")
public class Ciclo {

    @Id
    @Column(nullable = false, length = 10)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;
}
