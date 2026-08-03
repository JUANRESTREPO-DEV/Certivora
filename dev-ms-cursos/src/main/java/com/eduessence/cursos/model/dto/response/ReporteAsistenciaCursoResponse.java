package com.eduessence.cursos.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Resumen de asistencia por curso presencial. Fila del listado. */
@Data
@Builder
public class ReporteAsistenciaCursoResponse {
    private Long cursoId;
    private String nombre;
    private String slug;
    private String sede;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    private int totalSesiones;
    private long totalMatriculados;
    private long totalCheckins;

    /** matriculados × sesiones = total posibles. Usado para el %. */
    private long checkinsPosibles;

    /** 0..100. Redondeado a 1 decimal en el front. */
    private double porcentajeAsistencia;
}
