package com.eduessence.inbox.model.enums;

/** Naturaleza del buzón corporativo. */
public enum TipoBuzon {
    /** Buzón personal de un empleado/instructor. */
    PERSONAL,
    /** Buzón de área (soporte@, ventas@). */
    AREA,
    /** Asociado a un curso específico. */
    CURSO,
    /** Asociado a un evento Summit. */
    EVENTO,
    /** Buzón temporal con expiración. */
    TEMPORAL,
    /** Catch-all: recibe todo lo que no matchee otra dirección. */
    CATCHALL,
    /** Solo para enviar (no recibe). Ej: noreply@. */
    SOLO_SALIDA
}
