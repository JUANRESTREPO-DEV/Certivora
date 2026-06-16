package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.EstadoCurso;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "curso", indexes = {
        @Index(name = "uk_curso_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 220, unique = true)
    private String slug;

    @Column(name = "descripcion_corta", length = 500)
    private String descripcionCorta;

    @Column(name = "descripcion_larga", columnDefinition = "TEXT")
    private String descripcionLarga;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Video corto de bienvenida / trailer del curso (S3). */
    @Column(name = "video_presentacion_url", length = 500)
    private String videoPresentacionUrl;

    /** Imagen hero del curso (distinta del logo). */
    @Column(name = "imagen_presentacion_url", length = 500)
    private String imagenPresentacionUrl;

    /** Si el curso emite certificado al aprobarlo. */
    @Column(name = "emite_certificado", nullable = false)
    @Builder.Default
    private Boolean emiteCertificado = false;

    /** ID de la plantilla de certificado en dev-ms-certificados (FK lógica). */
    @Column(name = "template_certificado_id")
    private Long templateCertificadoId;

    /**
     * Temario / programa académico estructurado como JSON serializado.
     * Formato: {@code [{titulo: string, items: string[]}, ...]}.
     * Se renderiza como acordeón en la vista de detalle del curso.
     */
    @Column(name = "temario_json", columnDefinition = "TEXT")
    private String temarioJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCurso estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "fecha_limite_inscripcion")
    private LocalDateTime fechaLimiteInscripcion;

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Column(name = "precio_cop", nullable = false)
    private Integer precioCop;

    @Column(name = "intensidad_horas")
    private Integer intensidadHoras;

    /** Instructor principal — se mantiene por compatibilidad. La fuente real
     *  son los registros en {@link #instructores}. El service lo sincroniza
     *  con el que tenga principal=true. */
    @Column(name = "instructor_usuario_id")
    private Long instructorUsuarioId;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<CursoInstructor> instructores = new HashSet<>();

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<Seccion> secciones = new ArrayList<>();

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<CursoModalidad> modalidades = new HashSet<>();

    @OneToOne(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ConfiguracionAprobacion configuracionAprobacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
        if (this.estado == null) this.estado = EstadoCurso.BORRADOR;
        if (this.precioCop == null) this.precioCop = 0;
    }

    @PreUpdate
    void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    /** True si tiene ≥2 modalidades activas. */
    public boolean isHibrido() {
        return modalidades != null && modalidades.size() >= 2;
    }
}
