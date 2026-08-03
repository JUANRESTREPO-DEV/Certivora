package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.model.enums.TipoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    /* ─────────────────── consultas básicas ─────────────────── */

    Optional<Matricula> findByUsuarioIdAndCurso_Id(Long usuarioId, Long cursoId);

    List<Matricula> findAllByUsuarioId(Long usuarioId);

    long countByCurso_IdAndEstadoIn(Long cursoId, List<EstadoMatricula> estados);

    List<Matricula> findAllByCurso_Id(Long cursoId);

    List<Matricula> findAllByCurso_IdAndEstado(Long cursoId, EstadoMatricula estado);

    List<Matricula> findAllByCurso_IdAndTipo(Long cursoId, TipoParticipante tipo);

    /* ─────────────────── inscripción duplicada ───────────────────
       Ignoramos los estados terminales para permitir re-inscribirse después
       de cancelar o que se expirara la reserva. */

    @Query("""
            SELECT m FROM Matricula m
            WHERE m.usuarioId = :usuarioId
              AND m.curso.id  = :cursoId
              AND m.estado NOT IN (
                com.eduessence.cursos.model.enums.EstadoMatricula.CANCELADA,
                com.eduessence.cursos.model.enums.EstadoMatricula.REEMBOLSADA,
                com.eduessence.cursos.model.enums.EstadoMatricula.REPROBADA
              )
            """)
    Optional<Matricula> findMatriculaActiva(@Param("usuarioId") Long usuarioId,
                                            @Param("cursoId")   Long cursoId);

    /* ─────────────────── cupo ───────────────────
       Cuenta las inscripciones que ocupan cupo (PENDIENTE_PAGO + ACTIVA +
       APROBADA + FINALIZADA). Sirve para decidir si un nuevo usuario entra
       directo o va a lista de espera. */

    @Query("""
            SELECT COUNT(m) FROM Matricula m
            WHERE m.curso.id = :cursoId
              AND m.estado IN (
                com.eduessence.cursos.model.enums.EstadoMatricula.PENDIENTE_PAGO,
                com.eduessence.cursos.model.enums.EstadoMatricula.ACTIVA,
                com.eduessence.cursos.model.enums.EstadoMatricula.APROBADA,
                com.eduessence.cursos.model.enums.EstadoMatricula.FINALIZADA
              )
            """)
    long countOcupandoCupo(@Param("cursoId") Long cursoId);

    /* ─────────────────── lista de espera ─────────────────── */

    /** Máxima posición actual en la lista de espera del curso (para asignar la siguiente). */
    @Query("""
            SELECT COALESCE(MAX(m.posicionEspera), 0) FROM Matricula m
            WHERE m.curso.id = :cursoId
              AND m.estado   = com.eduessence.cursos.model.enums.EstadoMatricula.EN_ESPERA
            """)
    int maxPosicionEspera(@Param("cursoId") Long cursoId);

    /** Próximo en la cola: estado EN_ESPERA con menor {@code posicionEspera}. */
    @Query("""
            SELECT m FROM Matricula m
            WHERE m.curso.id = :cursoId
              AND m.estado   = com.eduessence.cursos.model.enums.EstadoMatricula.EN_ESPERA
            ORDER BY m.posicionEspera ASC, m.fechaInscripcion ASC
            """)
    List<Matricula> findCandidatosListaEspera(@Param("cursoId") Long cursoId);

    /* ─────────────────── reservas vencidas ───────────────────
       Cron job: matriculas PENDIENTE_PAGO con reserva_expira < now → cancelar
       y promover lista de espera. */

    @Query("""
            SELECT m FROM Matricula m
            WHERE m.estado = com.eduessence.cursos.model.enums.EstadoMatricula.PENDIENTE_PAGO
              AND m.reservaExpira IS NOT NULL
              AND m.reservaExpira < :ahora
            """)
    List<Matricula> findReservasVencidas(@Param("ahora") LocalDateTime ahora);

    Optional<Matricula> findByPagoId(Long pagoId);

    /** Lookup por token público de escarapela (endpoint /public/escarapela). */
    Optional<Matricula> findByEscarapelaToken(String token);
}
