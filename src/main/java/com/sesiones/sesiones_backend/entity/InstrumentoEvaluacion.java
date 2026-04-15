package com.sesiones.sesiones_backend.entity;

import com.sesiones.sesiones_backend.util.enums.InstrumentoTipo;

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
@Table(name = "instrumento_evaluacion")
public class InstrumentoEvaluacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private Sesion sesion;

    @Column(nullable = false, columnDefinition = "ENUM('rubrica','lista_cotejo')")
    private InstrumentoTipo tipo;

    @Column(name = "contenido_json", nullable = false, columnDefinition = "json")
    private String contenidoJson;
}
