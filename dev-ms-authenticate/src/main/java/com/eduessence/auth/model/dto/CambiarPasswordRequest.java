package com.eduessence.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body para {@code POST /api/perfil/cambiar-password} — el usuario cambia su
 * propia contraseña conociendo la actual. Alternativo al flujo de
 * {@code /recuperar-password} que exige un token de un solo uso por email.
 */
@Data
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = ".*[A-Z].*", message = "La nueva contraseña debe tener al menos una mayúscula")
    @Pattern(regexp = ".*\\d.*", message = "La nueva contraseña debe tener al menos un número")
    private String newPassword;
}
