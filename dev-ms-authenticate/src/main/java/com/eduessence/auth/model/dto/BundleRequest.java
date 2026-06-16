package com.eduessence.auth.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BundleRequest {
    /** GRANT | REVOKE — aplica todos los permisos requeridos de la acción. */
    @NotNull
    @Pattern(regexp = "GRANT|REVOKE")
    private String tipo;
}
