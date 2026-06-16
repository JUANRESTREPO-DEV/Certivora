package com.eduessence.cursos.model.enums;

/**
 * Ciclo de vida de una inscripción ({@code Matricula}).
 *
 * <pre>
 *           [crear]
 *  curso pago ─ con cupo ──► PENDIENTE_PAGO ── pago aprobado ──► ACTIVA
 *                                │  reserva expira / cancela
 *                                ▼
 *                            CANCELADA
 *  curso gratis ─ con cupo ──► ACTIVA
 *  cupo lleno ─────────────► EN_ESPERA ─ se libera cupo ──► PENDIENTE_PAGO / ACTIVA
 *  cualquiera ─ user cancela ─► CANCELADA
 *  ACTIVA ─ admin reembolsa ──► REEMBOLSADA
 *  ACTIVA ─ termina y aprueba ─► APROBADA / REPROBADA / FINALIZADA
 * </pre>
 */
public enum EstadoMatricula {
    /** Cupo reservado temporalmente, esperando confirmación de pago. */
    PENDIENTE_PAGO,
    /** Sin cupo al inscribirse — espera a que se libere uno. */
    EN_ESPERA,
    /** Inscripción confirmada. Puede acceder al contenido. */
    ACTIVA,
    /** Aprobó el curso (criterios de notas / progreso / asistencia cumplidos). */
    APROBADA,
    /** Reprobó. */
    REPROBADA,
    /** Suspendida por admin (deuda, comportamiento, etc.). */
    SUSPENDIDA,
    /** Curso terminó sin que se calcule aprobación (modalidad libre). */
    FINALIZADA,
    /** Usuario o sistema canceló antes de empezar — libera cupo. */
    CANCELADA,
    /** Admin reembolsó el pago — libera cupo. */
    REEMBOLSADA
}
