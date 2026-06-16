package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.ActualizarProgresoRequest;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;

public interface ProgresoService {

    /** Actualiza el progreso de una lección y recalcula el % global de la matrícula. */
    MatriculaResponse actualizar(Long matriculaId, Long leccionId, ActualizarProgresoRequest request);

    /**
     * Recalcula el {@code progreso_pct} de TODAS las matrículas del curso a
     * partir del total actual de lecciones. Se invoca cuando el admin agrega
     * o elimina lecciones, para que el % cacheado no quede desfasado.
     */
    void recalcularProgresoCurso(Long cursoId);
}
