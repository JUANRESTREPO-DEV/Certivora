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
public class TokenValidacionResponse {
    private Boolean valido;
    private Long userId;
    private String email;
    private Set<String> roles;
    private Long expiraEnSegundos;
}
