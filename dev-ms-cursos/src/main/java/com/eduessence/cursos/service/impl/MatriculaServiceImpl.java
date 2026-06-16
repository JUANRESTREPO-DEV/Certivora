package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.PagosServiceClient;
import com.eduessence.cursos.feign.SendmailServiceClient;
import com.eduessence.cursos.model.dto.request.CancelarMatriculaRequest;
import com.eduessence.cursos.model.dto.request.CortesiaRequest;
import com.eduessence.cursos.model.dto.request.InscribirRequest;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.CursoModalidad;
import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.enums.EstadoCurso;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.model.enums.TipoParticipante;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.service.MatriculaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orquestador del ciclo de vida de matrículas. Implementa las reglas
 * descritas en {@link MatriculaService} más la promoción automática de la
 * lista de espera cuando se libera un cupo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatriculaServiceImpl implements MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final PagosServiceClient pagosClient;
    private final SendmailServiceClient sendmail;

    /** Horas que dura la reserva temporal del cupo mientras se confirma el pago. */
    @Value("${app.matricula.reserva-horas:24}")
    private int reservaHoras;

    /* ──────────────────────────────────────────────────────────────────
     * FLUJO PÚBLICO: inscribir
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public MatriculaResponse inscribir(Long usuarioId, Long cursoId, InscribirRequest req) {
        Curso curso = findCurso(cursoId);

        // R-INS-01 → R-INS-04
        validarCursoAceptaInscripciones(curso);
        validarDuplicado(usuarioId, cursoId);
        validarModalidades(curso, req.getModalidades());

        boolean esPago = curso.getPrecioCop() != null && curso.getPrecioCop() > 0;
        boolean hayCupo = hayCupoDisponible(curso);

        // R-INS-05: cupo lleno → EN_ESPERA, sin pago. Cuando se libere se
        // notifica al user para que inicie el pago (si aplica).
        if (!hayCupo) {
            Matricula esperando = guardar(
                    construirBase(usuarioId, curso, req, TipoParticipante.ASISTENTE, false)
                            .estado(EstadoMatricula.EN_ESPERA)
                            .posicionEspera(matriculaRepository.maxPosicionEspera(cursoId) + 1)
                            .build(),
                    "matricula en lista de espera");
            return toResponse(esperando);
        }

        // R-INS-07: curso gratis → ACTIVA directo
        if (!esPago) {
            Matricula m = guardar(construirBase(usuarioId, curso, req, TipoParticipante.ASISTENTE, false)
                    .estado(EstadoMatricula.ACTIVA).build(), "matricula gratuita activada");
            notificar(m, curso, "CURSO_INSCRIPCION_OK");
            return toResponse(m);
        }

        // R-INS-06: curso pago — crear o reutilizar pago
        PagoResumen pago = resolverPago(usuarioId, curso, req);

        EstadoMatricula estado = pago.aprobado()
                ? EstadoMatricula.ACTIVA
                : EstadoMatricula.PENDIENTE_PAGO;

        Matricula m = guardar(construirBase(usuarioId, curso, req, TipoParticipante.ASISTENTE, false)
                .estado(estado)
                .pagoId(pago.pagoId())
                .reservaExpira(estado == EstadoMatricula.PENDIENTE_PAGO
                        ? LocalDateTime.now().plusHours(reservaHoras)
                        : null)
                .build(), "matricula con pago " + pago.estado());

        notificar(m, curso, estado == EstadoMatricula.ACTIVA
                ? "CURSO_INSCRIPCION_OK"
                : "CURSO_INSCRIPCION_PENDIENTE_PAGO");
        return toResponse(m);
    }

    /* ──────────────────────────────────────────────────────────────────
     * CORTESÍA (admin)
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public MatriculaResponse crearCortesia(Long adminUsuarioId, Long cursoId, CortesiaRequest req) {
        Curso curso = findCurso(cursoId);
        validarCursoEnEstado(curso, EstadoCurso.ACTIVO, EstadoCurso.BORRADOR);
        validarDuplicado(req.getUsuarioId(), cursoId);
        validarModalidades(curso, req.getModalidades());

        boolean ignorarCupo = Boolean.TRUE.equals(req.getIgnorarCupo())
                || req.getTipo() != TipoParticipante.ASISTENTE; // PONENTE/ORG/STAFF no cuentan
        if (!ignorarCupo && !hayCupoDisponible(curso)) {
            throw new CursosApiException(ServerApiStatusCode.CUPO_AGOTADO,
                    "Cupo de asistentes lleno. Usa ignorarCupo=true si la cortesía no debe ocupar lugar.");
        }

        InscribirRequest ir = new InscribirRequest();
        ir.setModalidades(req.getModalidades());

        Matricula m = guardar(construirBase(req.getUsuarioId(), curso, ir, req.getTipo(), true)
                .estado(EstadoMatricula.ACTIVA).build(),
                "cortesia tipo=" + req.getTipo() + " por admin=" + adminUsuarioId);

        notificar(m, curso, "CURSO_CORTESIA");
        return toResponse(m);
    }

    /* ──────────────────────────────────────────────────────────────────
     * ACTIVAR (webhook desde pagos cuando aprueba)
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public MatriculaResponse activar(Long matriculaId) {
        Matricula m = findMatricula(matriculaId);
        if (m.getEstado() == EstadoMatricula.PENDIENTE_PAGO) {
            m.setEstado(EstadoMatricula.ACTIVA);
            m.setReservaExpira(null);
            matriculaRepository.save(m);
            log.info("Matrícula {} activada por confirmación de pago", matriculaId);
            notificar(m, m.getCurso(), "CURSO_INSCRIPCION_OK");
        }
        return toResponse(m);
    }

    /* ──────────────────────────────────────────────────────────────────
     * CANCELAR (usuario o admin)
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public MatriculaResponse cancelar(Long matriculaId, Long quienCancela, CancelarMatriculaRequest req) {
        Matricula m = findMatricula(matriculaId);

        // Solo cancelable mientras no haya empezado y no esté ya en estado terminal
        if (!esCancelable(m.getEstado())) {
            throw new CursosApiException(ServerApiStatusCode.MATRICULA_NO_CANCELABLE,
                    "Estado actual " + m.getEstado() + " ya no se puede cancelar");
        }

        boolean liberaCupo = m.ocupaCupo();
        m.setEstado(EstadoMatricula.CANCELADA);
        m.setCanceladoMotivo(req == null ? null : req.getMotivo());
        m.setFechaCancelacion(LocalDateTime.now());
        m.setCanceladoPorUsuarioId(quienCancela);
        m.setReservaExpira(null);
        matriculaRepository.save(m);

        if (liberaCupo) promoverDesdeListaEspera(m.getCurso());
        notificar(m, m.getCurso(), "CURSO_INSCRIPCION_CANCELADA");
        return toResponse(m);
    }

    /* ──────────────────────────────────────────────────────────────────
     * REEMBOLSAR (admin)
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public MatriculaResponse reembolsar(Long matriculaId, Long adminUsuarioId, CancelarMatriculaRequest req) {
        Matricula m = findMatricula(matriculaId);
        if (m.getEstado() != EstadoMatricula.ACTIVA && m.getEstado() != EstadoMatricula.PENDIENTE_PAGO) {
            throw new CursosApiException(ServerApiStatusCode.MATRICULA_NO_REEMBOLSABLE,
                    "Solo ACTIVA o PENDIENTE_PAGO se pueden reembolsar (estado actual=" + m.getEstado() + ")");
        }
        m.setEstado(EstadoMatricula.REEMBOLSADA);
        m.setCanceladoMotivo(req == null ? null : req.getMotivo());
        m.setFechaCancelacion(LocalDateTime.now());
        m.setCanceladoPorUsuarioId(adminUsuarioId);
        m.setReservaExpira(null);
        matriculaRepository.save(m);

        promoverDesdeListaEspera(m.getCurso());
        notificar(m, m.getCurso(), "CURSO_INSCRIPCION_REEMBOLSADA");
        return toResponse(m);
    }

    /* ──────────────────────────────────────────────────────────────────
     * RESERVAS VENCIDAS (cron)
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public int liberarReservasVencidas() {
        List<Matricula> vencidas = matriculaRepository.findReservasVencidas(LocalDateTime.now());
        if (vencidas.isEmpty()) return 0;

        Set<Long> cursosAfectados = new HashSet<>();
        for (Matricula m : vencidas) {
            m.setEstado(EstadoMatricula.CANCELADA);
            m.setCanceladoMotivo("Reserva expirada sin confirmación de pago");
            m.setFechaCancelacion(LocalDateTime.now());
            m.setReservaExpira(null);
            matriculaRepository.save(m);
            cursosAfectados.add(m.getCurso().getId());
            notificar(m, m.getCurso(), "CURSO_INSCRIPCION_EXPIRADA");
        }
        log.info("Reservas liberadas: {} (cursos={})", vencidas.size(), cursosAfectados);

        // Promover lista de espera de cada curso afectado
        for (Long cid : cursosAfectados) {
            cursoRepository.findById(cid).ifPresent(this::promoverDesdeListaEspera);
        }
        return vencidas.size();
    }

    /* ──────────────────────────────────────────────────────────────────
     * CONSULTAS
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional(readOnly = true)
    public List<MatriculaResponse> misMatriculas(Long usuarioId) {
        return matriculaRepository.findAllByUsuarioId(usuarioId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MatriculaResponse obtener(Long matriculaId) {
        return toResponse(findMatricula(matriculaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorCurso(Long cursoId, EstadoMatricula estadoFiltro) {
        List<Matricula> data = estadoFiltro == null
                ? matriculaRepository.findAllByCurso_Id(cursoId)
                : matriculaRepository.findAllByCurso_IdAndEstado(cursoId, estadoFiltro);
        return data.stream().map(this::toResponse).toList();
    }

    /* ──────────────────────────────────────────────────────────────────
     * HELPERS
     * ────────────────────────────────────────────────────────────────── */

    private Curso findCurso(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO));
    }

    private Matricula findMatricula(Long id) {
        return matriculaRepository.findById(id)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.MATRICULA_NO_ENCONTRADA));
    }

    private void validarCursoAceptaInscripciones(Curso curso) {
        if (curso.getEstado() != EstadoCurso.ACTIVO) {
            throw new CursosApiException(ServerApiStatusCode.CURSO_NO_ACTIVO,
                    "El curso está en estado " + curso.getEstado());
        }
        if (curso.getFechaLimiteInscripcion() != null
                && LocalDateTime.now().isAfter(curso.getFechaLimiteInscripcion())) {
            throw new CursosApiException(ServerApiStatusCode.INSCRIPCION_CERRADA);
        }
    }

    private void validarCursoEnEstado(Curso curso, EstadoCurso... permitidos) {
        for (EstadoCurso p : permitidos) {
            if (curso.getEstado() == p) return;
        }
        throw new CursosApiException(ServerApiStatusCode.CURSO_NO_ACTIVO,
                "El curso debe estar en uno de " + List.of(permitidos));
    }

    private void validarDuplicado(Long usuarioId, Long cursoId) {
        matriculaRepository.findMatriculaActiva(usuarioId, cursoId).ifPresent(m -> {
            throw new CursosApiException(ServerApiStatusCode.YA_INSCRITO,
                    "Ya tienes una inscripción " + m.getEstado() + " en este curso");
        });
    }

    private void validarModalidades(Curso curso, Set<ModalidadTipo> elegidas) {
        Set<ModalidadTipo> ofrecidas = curso.getModalidades().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(CursoModalidad::getTipo).collect(Collectors.toSet());
        for (ModalidadTipo m : elegidas) {
            if (!ofrecidas.contains(m)) {
                throw new CursosApiException(ServerApiStatusCode.MODALIDAD_NO_DISPONIBLE,
                        "Modalidad " + m + " no está activa en el curso");
            }
        }
    }

    private boolean hayCupoDisponible(Curso curso) {
        if (curso.getCupoMaximo() == null) return true;
        long ocupado = matriculaRepository.countOcupandoCupo(curso.getId());
        return ocupado < curso.getCupoMaximo();
    }

    private boolean esCancelable(EstadoMatricula e) {
        return e == EstadoMatricula.PENDIENTE_PAGO
                || e == EstadoMatricula.EN_ESPERA
                || e == EstadoMatricula.ACTIVA;
    }

    /**
     * Llama a dev-ms-pagos para obtener (o iniciar) el pago. Retorna su id y
     * si está aprobado/gratis.
     */
    private PagoResumen resolverPago(Long usuarioId, Curso curso, InscribirRequest req) {
        try {
            Map<String, Object> data;
            if (req.getPagoId() != null) {
                Map<String, Object> wrap = pagosClient.obtenerPago(req.getPagoId());
                data = unwrap(wrap);
                if (data == null) data = wrap; // /internal no usa wrapper
                Long pagoId = req.getPagoId();
                String estado = String.valueOf(data.getOrDefault("estado", "DESCONOCIDO"));
                return new PagoResumen(pagoId, estado, esEstadoAprobado(estado));
            }

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("cursoId", curso.getId());
            body.put("montoCurso", curso.getPrecioCop());
            if (req.getCuponCodigo() != null && !req.getCuponCodigo().isBlank()) {
                body.put("cuponCodigo", req.getCuponCodigo());
            }
            Map<String, Object> wrap = pagosClient.iniciarPago(body);
            data = unwrap(wrap);
            if (data == null) {
                throw new CursosApiException(ServerApiStatusCode.PAGO_NO_DISPONIBLE,
                        "Pagos no devolvió response usable: " + wrap);
            }
            Long pagoId = ((Number) data.get("pagoId")).longValue();
            String estado = String.valueOf(data.get("estado"));
            return new PagoResumen(pagoId, estado, esEstadoAprobado(estado));
        } catch (CursosApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error integrando pago para usuario={} curso={}", usuarioId, curso.getId(), ex);
            throw new CursosApiException(ServerApiStatusCode.PAGO_NO_DISPONIBLE, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> wrap) {
        if (wrap == null) return null;
        Object resp = wrap.get("response");
        return resp instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private static boolean esEstadoAprobado(String estado) {
        return "APROBADO".equals(estado) || "GRATIS_POR_CUPON".equals(estado);
    }

    /**
     * Saca el siguiente {@code EN_ESPERA} del curso (FIFO) y lo promueve:
     * <ul>
     *   <li>Si el curso es gratis → {@code ACTIVA}.</li>
     *   <li>Si el curso es pago → {@code PENDIENTE_PAGO} con reserva 24h y se
     *       le notifica para que inicie el pago.</li>
     * </ul>
     * Idempotente: si no hay candidatos o no hay cupo, no hace nada.
     */
    private void promoverDesdeListaEspera(Curso curso) {
        if (!hayCupoDisponible(curso)) return;
        List<Matricula> candidatos = matriculaRepository.findCandidatosListaEspera(curso.getId());
        if (candidatos.isEmpty()) return;

        Matricula siguiente = candidatos.get(0);
        boolean esPago = curso.getPrecioCop() != null && curso.getPrecioCop() > 0;

        if (esPago) {
            siguiente.setEstado(EstadoMatricula.PENDIENTE_PAGO);
            siguiente.setReservaExpira(LocalDateTime.now().plusHours(reservaHoras));
            notificar(siguiente, curso, "CURSO_CUPO_LIBERADO_PAGAR");
        } else {
            siguiente.setEstado(EstadoMatricula.ACTIVA);
            notificar(siguiente, curso, "CURSO_INSCRIPCION_OK");
        }
        siguiente.setPosicionEspera(null);
        matriculaRepository.save(siguiente);
        log.info("Matrícula {} promovida desde lista de espera del curso {}",
                siguiente.getId(), curso.getId());
    }

    private Matricula.MatriculaBuilder construirBase(Long usuarioId, Curso curso,
                                                      InscribirRequest req,
                                                      TipoParticipante tipo,
                                                      boolean cortesia) {
        return Matricula.builder()
                .usuarioId(usuarioId)
                .curso(curso)
                .tipo(tipo)
                .cortesia(cortesia)
                .modalidades(new HashSet<>(req.getModalidades()));
    }

    private Matricula guardar(Matricula m, String mensajeLog) {
        Matricula saved = matriculaRepository.save(m);
        log.info("Matrícula {} guardada: {}", saved.getId(), mensajeLog);
        return saved;
    }

    private void notificar(Matricula m, Curso curso, String template) {
        try {
            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", template,
                    "destinatario", Map.of(
                            "correo", "usuario-" + m.getUsuarioId() + "@eduessence.local",
                            "nombre", "Usuario"),
                    "variables", Map.of(
                            "nombreCurso", curso.getNombre(),
                            "matriculaId", m.getId(),
                            "estado", m.getEstado().name(),
                            "fechaInicio", curso.getFechaInicio() == null
                                    ? "" : curso.getFechaInicio().toString())
            ));
        } catch (Exception ex) {
            log.warn("No se pudo notificar matrícula {} con template {}: {}",
                    m.getId(), template, ex.getMessage());
        }
    }

    private MatriculaResponse toResponse(Matricula m) {
        return MatriculaResponse.builder()
                .id(m.getId())
                .usuarioId(m.getUsuarioId())
                .cursoId(m.getCurso().getId())
                .cursoNombre(m.getCurso().getNombre())
                .estado(m.getEstado())
                .tipo(m.getTipo())
                .posicionEspera(m.getPosicionEspera())
                .reservaExpira(m.getReservaExpira())
                .pagoId(m.getPagoId())
                .cortesia(m.getCortesia())
                .modalidades(m.getModalidades())
                .progresoPct(m.getProgresoPct())
                .notaFinal(m.getNotaFinal())
                .fechaInscripcion(m.getFechaInscripcion())
                .fechaAprobacion(m.getFechaAprobacion())
                .fechaCancelacion(m.getFechaCancelacion())
                .canceladoMotivo(m.getCanceladoMotivo())
                .certificadoEmitido(m.getCertificadoEmitido())
                .build();
    }

    /** Tupla local — id de pago + estado + si ya está aprobado. */
    private record PagoResumen(Long pagoId, String estado, boolean aprobado) {}
}
