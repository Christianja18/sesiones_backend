package com.sesiones.sesiones_backend.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sesion")
public class Sesion extends CreationAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String proposito;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    private LocalDate fecha;

    @Column(name = "generado_por_ia", nullable = false)
    private boolean generadoPorIa;

    @ManyToMany
    @JoinTable(
        name = "sesion_competencia",
        joinColumns = @JoinColumn(name = "sesion_id"),
        inverseJoinColumns = @JoinColumn(name = "competencia_id")
    )
    private List<Competencia> competencias = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "sesion_capacidad",
        joinColumns = @JoinColumn(name = "sesion_id"),
        inverseJoinColumns = @JoinColumn(name = "capacidad_id")
    )
    private List<Capacidad> capacidades = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "sesion_desempeno",
        joinColumns = @JoinColumn(name = "sesion_id"),
        inverseJoinColumns = @JoinColumn(name = "desempeno_id")
    )
    private List<Desempeno> desempenos = new ArrayList<>();

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Actividad> actividades = new ArrayList<>();

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CriterioEvaluacion> criteriosEvaluacion = new ArrayList<>();

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evidencia> evidencias = new ArrayList<>();

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InstrumentoEvaluacion> instrumentosEvaluacion = new ArrayList<>();
}
