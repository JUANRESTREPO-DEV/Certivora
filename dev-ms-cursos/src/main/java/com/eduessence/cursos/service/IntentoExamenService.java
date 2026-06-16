package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.GuardarRespuestaRequest;
import com.eduessence.cursos.model.dto.response.IntentoExamenResponse;

import java.util.List;

/**
 * Ciclo de vida de un intento de examen para un alumno:
 * <ol>
 *   <li>{@code iniciar} — crea un nuevo {@link IntentoExamenResponse}.</li>
 *   <li>{@code guardarRespuesta} (N veces) — el alumno responde cada pregunta.</li>
 *   <li>{@code finalizar} — califica automáticamente y devuelve la nota.</li>
 *   <li>{@code obtener} — para releer el intento ya finalizado (review).</li>
 * </ol>
 */
public interface IntentoExamenService {

    /** Crea un nuevo intento. Falla si ya alcanzó {@code intentosMaximos}. */
    IntentoExamenResponse iniciar(Long matriculaId, Long examenId);

    /** Idempotente: si ya existe respuesta para esa pregunta, la actualiza. */
    IntentoExamenResponse guardarRespuesta(Long intentoId, GuardarRespuestaRequest req);

    /** Finaliza el intento: califica las preguntas y guarda el puntaje. */
    IntentoExamenResponse finalizar(Long intentoId);

    /** Devuelve el intento. Si {@code finalizado=true} incluye corrección. */
    IntentoExamenResponse obtener(Long intentoId);

    /** Lista los intentos del alumno para un examen, del más reciente al primero. */
    List<IntentoExamenResponse> listarPorMatriculaYExamen(Long matriculaId, Long examenId);
}
