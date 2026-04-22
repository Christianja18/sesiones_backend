package com.sesiones.sesiones_backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "documento_curriculo")
public class DocumentoCurriculo extends BaseEntity {

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "archivo_url", nullable = false, unique = true, length = 500)
    private String archivoUrl;

    @Column(name = "checksum_sha256", unique = true, length = 64)
    private String checksumSha256;

    @Column(nullable = false, columnDefinition = "ENUM('PENDIENTE','PROCESANDO','PROCESADO','ERROR')")
    private ProcesamientoEstado estado;

    @Column(name = "error_detalle", columnDefinition = "TEXT")
    private String errorDetalle;

    @CreationTimestamp
    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado;
}
