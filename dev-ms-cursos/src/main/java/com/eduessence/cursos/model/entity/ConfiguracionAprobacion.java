package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Parámetros de aprobación por curso.
 * Si una actividad/examen no define su {@code nota_minima_propia}, se usa
 * el {@code nota_minima_general} de aquí.
 */
@Entity
@Table(name = "configuracion_aprobacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionAprobacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false, unique = true)
    private Curso curso;

    /** Nota mínima general para exámenes y actividades (0-100). */
    @Column(name = "nota_minima_general", precision = 5, scale = 2, nullable = false)
    private BigDecimal notaMinimaGeneral;

    /** % mínimo de avance de lecciones para aprobar el curso. */
    @Column(name = "progreso_minimo_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal progresoMinimoPct;

    /** % mínimo de presencia (conectividad) en clases VIRTUAL_LIVE. */
    @Column(name = "presencia_minima_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal presenciaMinimaPct;

    /** % mínimo de asistencia registrada por QR en sesiones PRESENCIALES. */
    @Column(name = "presencia_qr_minima_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal presenciaQrMinimaPct;
}
