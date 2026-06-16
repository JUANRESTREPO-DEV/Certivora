package com.eduessence.cursos.model.enums;

/**
 * Tipo de participante para una {@code Matricula}.
 * <p>
 * Solo {@link #ASISTENTE} se obtiene por el flujo público de inscripción.
 * Los demás son <em>cortesías</em> creadas por administradores vía
 * {@code POST /api/admin/cursos/{cursoId}/inscripciones}.
 * </p>
 * <p>
 * El tipo determina qué plantilla de certificado se emite — ver
 * {@code template_certificado.tipo_participante} en dev-ms-certificados.
 * </p>
 */
public enum TipoParticipante {
    /** Inscripción comprada (o cupón / cortesía explícita). Flujo público. */
    ASISTENTE,
    /** Ponente del curso — recibe certificado de ponente. */
    PONENTE,
    /** Organizador del curso. */
    ORGANIZADOR,
    /** Staff de apoyo. */
    STAFF
}
