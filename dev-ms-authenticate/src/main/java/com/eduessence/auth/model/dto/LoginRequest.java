package com.eduessence.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Credenciales de login.
 *
 * El campo {@code username} acepta tanto el username como el email del usuario.
 * La lógica del service decide cuál buscar en base a la presencia del
 * carácter {@code @}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank
    @Size(min = 3, max = 150)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
