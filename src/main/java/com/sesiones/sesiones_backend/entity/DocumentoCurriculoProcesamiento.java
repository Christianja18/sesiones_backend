package com.sesiones.sesiones_backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.sesiones.sesiones_backend.util.enums.DocumentoProcesamientoEstado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "documento_curriculo_procesamiento")
public class DocumentoCurriculoProcesamiento extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_curriculo_id", nullable = false)
    private DocumentoCurriculo documentoCurriculo;

    @Column(nullable = false, length = 30)
    private DocumentoProcesamientoEstado estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_ultimo_proceso")
    private LocalDateTime fechaUltimoProceso;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
