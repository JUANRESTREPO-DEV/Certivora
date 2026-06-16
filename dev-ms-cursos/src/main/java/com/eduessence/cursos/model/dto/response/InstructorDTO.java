package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Instructor enriquecido con datos básicos del usuario.
 * El cursos-service trae estos datos vía Feign al authenticate-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorDTO {
    private Long usuarioId;
    private String nombres;
    private String apellidos;
    private String email;
    private String avatarUrl;
    private Boolean principal;
}
