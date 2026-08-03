package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CheckinScanRequest;
import com.eduessence.cursos.model.dto.response.AsistenteCheckinResponse;
import com.eduessence.cursos.model.dto.response.CheckinItemResponse;
import com.eduessence.cursos.model.dto.response.CheckinScanResponse;
import com.eduessence.cursos.model.dto.response.CursoCheckinResponse;
import com.eduessence.cursos.model.dto.response.SesionCheckinResponse;

import java.time.LocalDate;
import java.util.List;

public interface CheckinService {

    /**
     * Cursos presenciales de la vista inicial.
     *  - Si {@code soloParaUsuarioId} != null: solo cursos donde el user
     *    tiene matrícula PRESENCIAL activa (modo asistente). Incluye
     *    {@code misCheckins} con el conteo del user.
     *  - Si es null: todos los cursos con al menos una sesión PRESENCIAL
     *    (modo operador).
     */
    List<CursoCheckinResponse> misCursosPresenciales(Long soloParaUsuarioId);

    /**
     * Todos los check-ins registrados en el curso (todas sus sesiones).
     * Se muestra en el panel derecho a todos los users. Ordenado por
     * {@code fechaCheckin} DESC.
     */
    List<CheckinItemResponse> checkinsDelCurso(Long cursoId);

    /**
     * Sesiones PRESENCIAL en el rango [fecha 00:00, fecha 23:59].
     * Si {@code soloParaUsuarioId} no es null, filtra a las sesiones cuyos
     * cursos tengan una matrícula PRESENCIAL activa del user — para el modo
     * asistente que solo ve las sesiones de sus cursos.
     */
    List<SesionCheckinResponse> sesionesDeDia(LocalDate fecha, Long soloParaUsuarioId);

    /**
     * Todas las sesiones PRESENCIAL del curso con contadores y —si es asistente—
     * el propio estado de check-in por sesión. Cada sesión = 1 check disponible.
     */
    List<SesionCheckinResponse> sesionesDelCurso(Long cursoId, Long soloParaUsuarioId);

    /** Asistentes matriculados de un curso con status de check-in a la sesión. */
    List<AsistenteCheckinResponse> asistentesDeSesion(Long sesionId);

    /**
     * Devuelve SOLO la fila del user logueado — para modo asistente que solo
     * ve su propio estado en la sesión. Null si el user no está matriculado
     * al curso de la sesión.
     */
    AsistenteCheckinResponse miAsistenciaEnSesion(Long sesionId, Long usuarioId);

    /**
     * Registra un check-in. Soporta 2 modos:
     *  - Scan del QR de escarapela (recomendado): trae {@code escarapelaToken}
     *    y {@code sesionId} explícito.
     *  - Manual: {@code matriculaId} + {@code sesionId} desde el listado.
     */
    CheckinScanResponse scan(CheckinScanRequest req, Long operadorUsuarioId);
}
