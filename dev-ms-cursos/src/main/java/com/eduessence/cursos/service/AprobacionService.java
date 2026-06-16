package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.response.AprobacionResultDTO;

public interface AprobacionService {

    /**
     * Calcula si la matrícula cumple todos los criterios de aprobación:
     *  - Progreso (% lecciones completadas)  ≥ progreso_minimo_pct
     *  - Nota promedio de exámenes           ≥ nota_minima_general (o por examen)
     *  - Nota promedio de actividades        ≥ nota_minima_general (o por actividad)
     *  - % presencia en sesiones virtuales   ≥ presencia_minima_pct
     *
     * Devuelve el detalle del cálculo y si {@code aprobado=true}.
     * Si todo se cumple, marca la matrícula como APROBADA y dispara
     * el evento para que dev-ms-certificados emita el diploma.
     */
    AprobacionResultDTO evaluar(Long matriculaId);
}
