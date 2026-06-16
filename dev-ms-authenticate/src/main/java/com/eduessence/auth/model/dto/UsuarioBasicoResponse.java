package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload mínimo para pickers / autocompletes que solo necesitan
 * id + nombre completo + email. No expone roles ni permisos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioBasicoResponse {
    private Long id;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private String avatarUrl;
}
