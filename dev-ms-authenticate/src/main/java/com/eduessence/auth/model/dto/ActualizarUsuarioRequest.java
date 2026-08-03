package com.eduessence.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Payload para editar un usuario existente. Todos los campos son opcionales
 * (patch parcial). El email y username tienen validación de formato/tamaño
 * cuando vienen presentes.
 */
@Data
public class ActualizarUsuarioRequest {

    @Email
    @Size(max = 150)
    private String email;

    @Size(min = 3, max = 60)
    private String username;

    @Size(max = 120) private String nombres;
    @Size(max = 120) private String apellidos;
    @Size(max = 10)  private String tipoDocumento;
    @Size(max = 30)  private String documento;
    @Size(max = 30)  private String telefono;
    @Size(max = 60)  private String pais;
    @Size(max = 80)  private String ciudad;
    @Size(max = 100) private String profesion;
    @Size(max = 150) private String institucion;

    /** Nuevo estado (ACTIVO/INACTIVO/BLOQUEADO). Opcional. */
    private String estado;

    /**
     * Reemplaza el set de roles. Si es {@code null}, no toca los roles;
     * si es lista vacía, deja el usuario sin roles.
     */
    private List<String> roles;
}
