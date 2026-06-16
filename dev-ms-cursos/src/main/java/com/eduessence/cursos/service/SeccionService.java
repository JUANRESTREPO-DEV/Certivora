package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CrearSeccionRequest;
import com.eduessence.cursos.model.dto.response.SeccionResponse;

import java.util.List;

public interface SeccionService {
    List<SeccionResponse> listar(Long cursoId);
    SeccionResponse crear(Long cursoId, CrearSeccionRequest req);
    SeccionResponse actualizar(Long cursoId, Long seccionId, CrearSeccionRequest req);
    void borrar(Long cursoId, Long seccionId);
    void reordenar(Long cursoId, List<Long> seccionIdsEnOrden);
}
