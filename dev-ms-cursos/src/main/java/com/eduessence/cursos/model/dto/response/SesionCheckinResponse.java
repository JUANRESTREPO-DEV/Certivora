package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sesión presencial que aparece en el selector del panel de check-in.
 * Incluye contadores para mostrar progreso en el chip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionCheckinResponse {
    private Long sesionId;
    private Long cursoId;
    private String cursoNombre;
    private String titulo;
    private LocalDateTime fechaInicio;
    private Integer duracionMinutos;
    private String sede;
    private Boolean cancelada;

    /** Token opaco del QR — se usa en la URL /app/eventos/scan/{qrToken}. */
    private String qrToken;

    /** Total de matriculados con acceso PRESENCIAL en el curso. */
    private long totalMatriculados;
    /** Cuántos ya hicieron check-in. */
    private long totalCheckin;

    /** Estado propio del user autenticado (solo cuando el endpoint filtra por él). */
    private Boolean miCheckin;
    private java.time.LocalDateTime miFechaCheckin;
}
