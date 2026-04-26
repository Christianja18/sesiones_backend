package com.sesiones.sesiones_backend.entity;

import java.util.ArrayList;
import java.util.List;

import com.sesiones.sesiones_backend.util.enums.DesempenoFuente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "desempeno")
public class Desempeno extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, columnDefinition = "ENUM('oficial','ia')")
    private DesempenoFuente fuente = DesempenoFuente.OFICIAL;

    @ManyToMany
    @JoinTable(
        name = "desempeno_capacidad",
        joinColumns = @JoinColumn(name = "desempeno_id"),
        inverseJoinColumns = @JoinColumn(name = "capacidad_id")
    )
    private List<Capacidad> capacidades = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "estandar_desempeno",
        joinColumns = @JoinColumn(name = "desempeno_id"),
        inverseJoinColumns = @JoinColumn(name = "estandar_id")
    )
    private List<EstandarAprendizaje> estandares = new ArrayList<>();
}
