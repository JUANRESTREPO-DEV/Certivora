package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CrearRecursoRequest;
import com.eduessence.cursos.model.dto.response.RecursoResponse;
import com.eduessence.cursos.model.enums.TipoRecurso;

import java.util.List;

public interface CursoRecursoService {

    List<RecursoResponse> listar(Long cursoId);

    List<RecursoResponse> listarPorTipo(Long cursoId, TipoRecurso tipo);

    RecursoResponse crear(Long cursoId, CrearRecursoRequest req);

    RecursoResponse actualizar(Long cursoId, Long recursoId, CrearRecursoRequest req);

    void borrar(Long cursoId, Long recursoId);

    /** Reordena los recursos según el orden de la lista pasada (índices = orden). */
    void reordenar(Long cursoId, List<Long> recursoIdsEnOrden);
}
