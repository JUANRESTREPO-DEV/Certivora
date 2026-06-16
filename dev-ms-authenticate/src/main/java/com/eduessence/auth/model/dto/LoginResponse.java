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
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private Set<String> roles;
    private Set<String> permisos;
}
