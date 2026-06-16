package com.eduessence.inbox.model.enums;

/** Nivel de acceso de un usuario sobre un buzón. */
public enum RolBuzon {
    LECTOR,        // solo leer
    RESPONDER,     // leer + responder
    ADMIN_BUZON    // todo (incluye etiquetar/archivar/agregar accesos)
}
