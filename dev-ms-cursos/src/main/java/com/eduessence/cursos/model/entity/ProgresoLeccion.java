package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.EstadoLeccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progreso_leccion", uniqueConstraints = @UniqueConstraint(
        name = "uk_pl_matricula_leccion",
        columnNames = {"matricula_id", "leccion_id"}
), indexes = @Index(name = "idx_pl_matricula", columnList = "matricula_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgresoLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false)
    private Long matriculaId;

    @Column(name = "leccion_id", nullable = false)
    private Long leccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLeccion estado;

    /** Segundos vistos (para VIDEO). */
    @Column(name = "segundos_vistos", nullable = false)
    private Integer segundosVistos;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    @PrePersist
    void onCreate() {
        if (estado == null) estado = EstadoLeccion.NO_INICIADA;
        if (segundosVistos == null) segundosVistos = 0;
        fechaActualizacion = LocalDateTime.now();
        if (fechaInicio == null) fechaInicio = fechaActualizacion;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
