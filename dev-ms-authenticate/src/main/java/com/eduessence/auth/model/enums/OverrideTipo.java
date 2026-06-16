package com.eduessence.auth.model.enums;

/**
 * Override individual sobre los permisos heredados del rol:
 *   ALLOW — concede el permiso aunque el rol no lo tenga
 *   DENY  — revoca el permiso aunque el rol lo tenga
 */
public enum OverrideTipo {
    ALLOW,
    DENY
}
