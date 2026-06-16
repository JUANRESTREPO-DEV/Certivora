package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CrearLeccionRequest;
import com.eduessence.cursos.model.dto.response.LeccionResponse;

import java.util.List;

public interface LeccionService {
    List<LeccionResponse> listar(Long seccionId);
    LeccionResponse crear(Long seccionId, CrearLeccionRequest req);
    LeccionResponse actualizar(Long seccionId, Long leccionId, CrearLeccionRequest req);
    void borrar(Long seccionId, Long leccionId);
    void reordenar(Long seccionId, List<Long> leccionIdsEnOrden);
}
