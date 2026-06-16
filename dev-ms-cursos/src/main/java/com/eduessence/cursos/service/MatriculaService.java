package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.CancelarMatriculaRequest;
import com.eduessence.cursos.model.dto.request.CortesiaRequest;
import com.eduessence.cursos.model.dto.request.InscribirRequest;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.model.enums.EstadoMatricula;

import java.util.List;

public interface MatriculaService {

    /**
     * Flujo público de inscripción ({@code tipo = ASISTENTE}). Reglas:
     * <ul>
     *   <li>R-INS-01 No se puede inscribir 2 veces (filtra estados terminales).</li>
     *   <li>R-INS-02 Solo cursos {@code ACTIVO} aceptan inscripciones.</li>
     *   <li>R-INS-03 Respetar {@code fechaLimiteInscripcion}.</li>
     *   <li>R-INS-04 Modalidades elegidas ⊆ modalidades activas del curso.</li>
     *   <li>R-INS-05 Si {@code cupoMaximo} está lleno → {@code EN_ESPERA}.</li>
     *   <li>R-INS-06 Si curso pago → crea/asocia pago y deja en {@code PENDIENTE_PAGO}
     *       con {@code reservaExpira = now + 24h}.</li>
     *   <li>R-INS-07 Si cupón cubre 100% o curso gratis → entra {@code ACTIVA} directo.</li>
     * </ul>
     */
    MatriculaResponse inscribir(Long usuarioId, Long cursoId, InscribirRequest request);

    /**
     * Inscripción de cortesía creada por admin. Acepta cualquier
     * {@code TipoParticipante} y permite saltarse el cupo
     * ({@link CortesiaRequest#getIgnorarCupo()}). Nunca crea pago.
     */
    MatriculaResponse crearCortesia(Long adminUsuarioId, Long cursoId, CortesiaRequest request);

    /** Promueve {@code PENDIENTE_PAGO → ACTIVA}. Llamado por el webhook de pagos. */
    MatriculaResponse activar(Long matriculaId);

    /** Cancela. Si está en estado que ocupa cupo, promueve a la lista de espera. */
    MatriculaResponse cancelar(Long matriculaId, Long quienCancela, CancelarMatriculaRequest req);

    /** Admin marca como REEMBOLSADA. Libera cupo y promueve lista de espera. */
    MatriculaResponse reembolsar(Long matriculaId, Long adminUsuarioId, CancelarMatriculaRequest req);

    /**
     * Job programado: libera reservas vencidas (PENDIENTE_PAGO con
     * {@code reservaExpira < now}). Devuelve cuántas se liberaron.
     */
    int liberarReservasVencidas();

    /* ─── consultas ─── */

    List<MatriculaResponse> misMatriculas(Long usuarioId);

    MatriculaResponse obtener(Long matriculaId);

    /** Listado admin con filtro opcional por estado. */
    List<MatriculaResponse> listarPorCurso(Long cursoId, EstadoMatricula estadoFiltro);
}
