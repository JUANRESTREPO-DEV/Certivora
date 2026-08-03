package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Fila del listado paginado de usuarios en el back-office. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioListResponse {
    private Long id;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private String avatarUrl;
    private String telefono;
    private String documento;

    private String estado;
    private Boolean emailVerificado;

    private LocalDateTime ultimoAcceso;
    private LocalDateTime fechaCreacion;

    /** Códigos de rol asignados (ej: ["ADMIN", "INSTRUCTOR"]). */
    private List<String> roles;
}
