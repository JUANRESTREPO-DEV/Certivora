package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Plantilla visual de la escarapela (badge) del asistente en modalidad
 * PRESENCIAL de un curso. Igual espíritu que {@code DiplomaTemplate} en
 * dev-ms-certificados: un fondo (PNG/JPG en S3) más un JSON de posiciones
 * relativas de las variables que el editor visual del admin arrastró al
 * lienzo.
 *
 * <p>Variables soportadas por el render:
 * {@code nombre, evento, sede, fechaInicio, qrEscarapela}.</p>
 */
@Entity
@Table(name = "escarapela_template", uniqueConstraints = @UniqueConstraint(
        name = "uk_esc_curso", columnNames = "curso_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscarapelaTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    /** URL pública (S3) del fondo del badge. Puede ser null (lienzo blanco). */
    @Column(name = "fondo_url", length = 500)
    private String fondoUrl;

    /** JSON serializado con las posiciones/estilos de cada variable en el lienzo. */
    @Column(name = "posiciones_json", columnDefinition = "TEXT")
    private String posicionesJson;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (activo == null) activo = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
