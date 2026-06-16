package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectivePermissions {
    private Long usuarioId;
    private String email;
    private String username;
    private Set<String> roles;
    /** Permisos efectivos: roles ∪ ALLOW \ DENY. */
    private Set<String> permisos;
    /** Permisos heredados de los roles (sin aplicar overrides). */
    private Set<String> permisosBase;
    /** Permisos agregados explícitamente al usuario (ALLOW). */
    private Set<String> overridesAllow;
    /** Permisos revocados explícitamente al usuario (DENY). */
    private Set<String> overridesDeny;
    /** True si el usuario tiene rol ADMIN (concede todo). */
    private boolean admin;
}
