package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rol para dropdowns/pickers en el back-office. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private long totalUsuarios;
}
