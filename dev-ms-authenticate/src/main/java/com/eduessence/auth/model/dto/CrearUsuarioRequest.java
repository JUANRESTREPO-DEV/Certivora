package com.eduessence.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Payload para crear un usuario desde el back-office.
 *
 * El admin NO define username ni password:
 *   · El {@code username} se auto-genera a partir de nombres + apellidos.
 *   · La contraseña la define el propio usuario a través del enlace de
 *     "establecer contraseña" que le llega por email tras el alta.
 */
@Data
public class CrearUsuarioRequest {

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(max = 120)
    private String nombres;

    @NotBlank
    @Size(max = 120)
    private String apellidos;

    @Size(max = 10) private String tipoDocumento;
    @Size(max = 30) private String documento;
    @Size(max = 30) private String telefono;
    @Size(max = 60) private String pais;
    @Size(max = 80) private String ciudad;
    @Size(max = 100) private String profesion;
    @Size(max = 150) private String institucion;

    /** Códigos de rol a asignar. Si viene vacío, queda como ASISTENTE. */
    private List<String> roles;
}
