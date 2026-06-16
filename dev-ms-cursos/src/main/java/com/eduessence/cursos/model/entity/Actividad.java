package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividad", indexes = @Index(name = "idx_actividad_curso", columnList = "curso_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String instrucciones;

    /** Fecha límite (puede ser null = sin límite). */
    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    /** Nota mínima para aprobar esta actividad (0-100). Si NULL → cae al curso. */
    @Column(name = "nota_minima_propia", precision = 5, scale = 2)
    private BigDecimal notaMinimaPropia;

    /** Permite re-entregas? */
    @Column(name = "permite_reentrega", nullable = false)
    private Boolean permiteReentrega;

    @PrePersist
    void onCreate() {
        if (permiteReentrega == null) permiteReentrega = false;
    }
}
