package com.eduessence.auth.model.enums;

/**
 * Estado computado de una acción para un usuario:
 *   GRANTED — usuario tiene TODOS los permisos requeridos del bundle
 *   PARTIAL — usuario tiene ALGUNOS requeridos pero no todos
 *   DENIED  — usuario no tiene ninguno de los requeridos
 */
public enum AccionEstado {
    GRANTED,
    PARTIAL,
    DENIED
}
