package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.ActualizarConfigAprobacionRequest;
import com.eduessence.cursos.model.dto.response.ConfigAprobacionResponse;

public interface ConfigAprobacionService {
    ConfigAprobacionResponse obtener(Long cursoId);
    ConfigAprobacionResponse actualizar(Long cursoId, ActualizarConfigAprobacionRequest req);
}
