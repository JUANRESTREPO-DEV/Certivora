package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.EscarapelaTemplateRequest;
import com.eduessence.cursos.model.dto.response.EscarapelaRenderResponse;
import com.eduessence.cursos.model.dto.response.EscarapelaTemplateResponse;

public interface EscarapelaService {

    /** Devuelve la plantilla del curso o null si no existe. */
    EscarapelaTemplateResponse obtenerPorCurso(Long cursoId);

    /**
     * Crea o actualiza la plantilla del curso (upsert por curso_id).
     * El {@code activo=false} desactiva la escarapela para el curso.
     */
    EscarapelaTemplateResponse upsert(Long cursoId, EscarapelaTemplateRequest req);

    /**
     * Payload para el render público en {@code /public/escarapela/{token}}.
     * Resuelve datos del asistente y del curso.
     */
    EscarapelaRenderResponse renderPorToken(String token);
}
