package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.TipoLeccion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leccion", indexes = @Index(name = "idx_leccion_seccion", columnList = "seccion_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoLeccion tipo;

    @Column(nullable = false)
    private Integer orden;

    /** Si true: debe estar COMPLETADA para avanzar. */
    @Column(nullable = false)
    private Boolean bloqueante;

    /** Duración estimada en segundos (para VIDEO y SESION_VIRTUAL). */
    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    /** URL del recurso (video S3, PDF S3, etc.). */
    @Column(name = "recurso_url", length = 500)
    private String recursoUrl;

    /** Contenido en texto cuando tipo=TEXTO (HTML/Markdown). */
    @Column(columnDefinition = "TEXT")
    private String contenidoTexto;

    /**
     * Vínculo a la entidad detalle según el tipo:
     *  - EXAMEN          → examen_id
     *  - ACTIVIDAD       → actividad_id
     *  - SESION_VIRTUAL  → sesion_virtual_id
     * (referencias opcionales mantenidas vía FKs en tablas hijas; aquí solo
     * guardamos los ids para evitar joins complejos al listar)
     */
    @Column(name = "examen_id")
    private Long examenId;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "sesion_virtual_id")
    private Long sesionVirtualId;

    @PrePersist
    void onCreate() {
        if (bloqueante == null) bloqueante = false;
    }
}
