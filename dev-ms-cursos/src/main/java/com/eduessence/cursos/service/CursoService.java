package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.ActualizarCursoRequest;
import com.eduessence.cursos.model.dto.request.CrearCursoRequest;
import com.eduessence.cursos.model.dto.response.CursoResponse;
import com.eduessence.cursos.model.enums.EstadoCurso;

import java.util.List;

public interface CursoService {
    CursoResponse crear(CrearCursoRequest request);
    CursoResponse actualizar(Long id, ActualizarCursoRequest request);
    CursoResponse obtener(Long id);
    CursoResponse obtenerPorSlug(String slug);
    List<CursoResponse> listarActivos();
    /**
     * Listado admin — devuelve todos los estados o filtra por uno.
     * Para uso de usuarios con CURSO_VER_ADMIN/CURSO_LISTAR (gerentes/admins).
     */
    List<CursoResponse> listarTodos(EstadoCurso estadoFiltro);
    CursoResponse activar(Long id);
    CursoResponse archivar(Long id);
}
