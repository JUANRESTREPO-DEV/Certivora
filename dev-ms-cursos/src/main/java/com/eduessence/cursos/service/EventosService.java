package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.response.EventoResponse;
import com.eduessence.cursos.model.enums.TipoSesion;

import java.time.LocalDateTime;
import java.util.List;

public interface EventosService {

    /**
     * Próximos eventos públicos (sesiones de cursos ACTIVO no canceladas)
     * desde "ahora" hasta {@code horizonteHoras} adelante, ordenados por fecha.
     */
    List<EventoResponse> listarPublicosProximos(int horizonteDias, int limite);

    /**
     * Eventos para el back-office con filtros. Cualquier filtro {@code null}
     * se ignora.
     */
    List<EventoResponse> listarAdmin(
            LocalDateTime desde,
            LocalDateTime hasta,
            TipoSesion tipo,
            Long cursoId,
            String search,
            Boolean incluirCanceladas
    );
}
