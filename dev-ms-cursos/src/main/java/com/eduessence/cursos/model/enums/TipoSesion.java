package com.eduessence.cursos.model.enums;

/**
 * Una sesión del curso es siempre presencial o virtual. Aplica tanto a
 * {@code sesion_virtual} (que históricamente solo manejaba VIRTUAL) como a
 * sesiones presenciales con registro por QR.
 */
public enum TipoSesion {
    VIRTUAL,
    PRESENCIAL
}
