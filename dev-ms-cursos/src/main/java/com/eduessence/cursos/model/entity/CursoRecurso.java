package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.TipoRecurso;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Adjunto del curso (PDFs de programa, material descargable, etc.). Vive en S3;
 * aquí guardamos sólo la metadata y la {@code s3_key}.
 */
@Entity
@Table(name = "curso_recurso", indexes = {
        @Index(name = "idx_cr_curso", columnList = "curso_id"),
        @Index(name = "idx_cr_curso_tipo", columnList = "curso_id, tipo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoRecurso tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    /** URL pre-firmada con caducidad o URL pública si el bucket lo permite. */
    @Column(name = "url_publica", length = 700)
    private String urlPublica;

    @Column(length = 120)
    private String mime;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "nombre_original", length = 300)
    private String nombreOriginal;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.orden == null) this.orden = 0;
    }
}
