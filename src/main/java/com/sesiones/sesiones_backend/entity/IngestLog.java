package com.sesiones.sesiones_backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.sesiones.sesiones_backend.util.enums.IngestLogEstado;

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
@Table(name = "ingest_log")
public class IngestLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private DocumentoCurriculo documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private DocumentoChunk chunk;

    @Column(nullable = false, columnDefinition = "ENUM('OK','ERROR')")
    private IngestLogEstado estado;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
