package com.eduessence.auth.model.enums;

/**
 * Tipo de acción según su efecto visual en el front:
 *   PAGE    — pantalla completa (gating de toda la ruta)
 *   SECTION — sección dentro de una pantalla
 *   BUTTON  — botón / item de menú / control individual
 */
public enum AccionTipo {
    PAGE,
    SECTION,
    BUTTON
}
