package com.sesiones.sesiones_backend.entity;

import com.sesiones.sesiones_backend.util.enums.ProcesamientoEstado;

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
@Table(name = "documento_chunk")
public class DocumentoChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoCurriculo documento;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "pagina_inicio")
    private Integer paginaInicio;

    @Column(name = "pagina_fin")
    private Integer paginaFin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "hash_contenido", nullable = false, length = 64)
    private String hashContenido;

    @Column(nullable = false, columnDefinition = "ENUM('PENDIENTE','PROCESANDO','PROCESADO','ERROR')")
    private ProcesamientoEstado estado;
}
