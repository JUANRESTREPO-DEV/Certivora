package com.eduessence.cursos.model.enums;

/**
 * Modalidades soportadas. Un curso puede tener N modalidades (≥1).
 * "HIBRIDO" no es un valor: es el estado de tener ≥2 modalidades activas.
 */
public enum ModalidadTipo {
    PRESENCIAL,
    VIRTUAL_LIVE,
    GRABADO
}
