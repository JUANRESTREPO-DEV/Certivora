package com.eduessence.certificados.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Template de diploma asociado a un curso (y opcionalmente a un tipo de
 * participante). El PDF base vive en S3; las posiciones de los placeholders
 * (nombre, fecha, código, QR) se guardan en {@code posiciones} como JSON.
 */
@Entity
@Table(name = "diploma_template", indexes = {
        @Index(name = "idx_dt_curso", columnList = "curso_id"),
        @Index(name = "idx_dt_nombre", columnList = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiplomaTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Curso al que pertenece. {@code null} = plantilla global reutilizable
     * (creada por el admin desde el form de curso para asignarla a varios).
     */
    @Column(name = "curso_id")
    private Long cursoId;

    @Column(name = "tipo_participante", nullable = false, length = 40)
    @Builder.Default
    private String tipoParticipante = "ASISTENTE";

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** Key S3 de la imagen/PDF base del diploma. */
    @Column(name = "template_url", nullable = false, length = 500)
    private String templateUrl;

    /** JSON con coordenadas: {"nombre":{"x":120,"y":340,"fontSize":24},...}. */
    @Column(name = "posiciones", columnDefinition = "TEXT")
    private String posiciones;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
