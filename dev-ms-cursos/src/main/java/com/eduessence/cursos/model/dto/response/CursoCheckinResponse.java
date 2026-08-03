package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Card de curso presencial en la vista inicial del panel de check-in.
 * Se muestra a todos los users. Para asistentes, {@code miCheckins} refleja
 * cuántas sesiones del curso ya tienen check-in.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoCheckinResponse {
    private Long cursoId;
    private String nombre;
    private String slug;
    private String sede;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    /** Total de sesiones PRESENCIAL del curso. */
    private int totalSesiones;

    /** Total de check-ins registrados en TODAS las sesiones (para admin). */
    private long totalCheckins;

    /** Total de asistentes matriculados con acceso PRESENCIAL. */
    private long totalMatriculados;

    /** Solo cuando el user es asistente: cuántas sesiones tiene con check-in propio. */
    private Integer misCheckins;
}
