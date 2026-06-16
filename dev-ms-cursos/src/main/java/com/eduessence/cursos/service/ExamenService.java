package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CrearExamenRequest;
import com.eduessence.cursos.model.dto.response.ExamenResponse;

import java.util.List;

public interface ExamenService {
    List<ExamenResponse> listarPorCurso(Long cursoId);
    ExamenResponse obtener(Long cursoId, Long examenId);
    ExamenResponse crear(Long cursoId, CrearExamenRequest req);
    ExamenResponse actualizar(Long cursoId, Long examenId, CrearExamenRequest req);
    void borrar(Long cursoId, Long examenId);
}
