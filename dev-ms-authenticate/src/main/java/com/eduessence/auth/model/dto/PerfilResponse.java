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
public class PerfilResponse {
    private Long id;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private String documento;
    private String tipoDocumento;
    private String telefono;
    private String pais;
    private String ciudad;
    private String profesion;
    private String institucion;
    private String avatarUrl;
    private Boolean emailVerificado;
    private Set<String> roles;
}
