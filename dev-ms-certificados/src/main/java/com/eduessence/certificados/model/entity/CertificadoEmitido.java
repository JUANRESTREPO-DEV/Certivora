package com.eduessence.certificados.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Certificado emitido para un usuario en un curso. Una matrícula = un código.
 * El PDF final vive en S3; aquí guardamos solo metadata + hash para
 * verificación pública.
 */
@Entity
@Table(name = "certificado_emitido", uniqueConstraints = @UniqueConstraint(
        name = "uk_ce_codigo", columnNames = "codigo"
), indexes = {
        @Index(name = "idx_ce_usuario", columnList = "usuario_id"),
        @Index(name = "idx_ce_curso", columnList = "curso_id"),
        @Index(name = "idx_ce_matricula", columnList = "matricula_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificadoEmitido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false)
    private Long matriculaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    /** Código único visible: EDU-2026-000123. */
    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "nombre_curso", nullable = false, length = 200)
    private String nombreCurso;

    /** URL pre-firmada del PDF en S3. */
    @Column(name = "url_pdf", nullable = false, length = 500)
    private String urlPdf;

    /** URL pública de verificación con el código en query. */
    @Column(name = "qr_url", nullable = false, length = 500)
    private String qrUrl;

    /** SHA-256 de (usuario_id|curso_id|fecha_emision|secret). */
    @Column(name = "hash_verificacion", nullable = false, length = 64)
    private String hashVerificacion;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @PrePersist
    void onCreate() {
        if (fechaEmision == null) fechaEmision = LocalDateTime.now();
    }
}
