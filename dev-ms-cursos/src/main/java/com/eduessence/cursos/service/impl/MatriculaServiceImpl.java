package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.AuthenticateServiceClient;
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

import java.math.BigDecimal;
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
    private final AuthenticateServiceClient authClient;

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

        ModalidadResuelta res = resolverModalidad(curso, req);
        boolean esPago = res.precioCompra != null && res.precioCompra.signum() > 0;
        boolean hayCupo = hayCupoDisponible(curso);

        // R-INS-05: cupo lleno → EN_ESPERA, sin pago.
        if (!hayCupo) {
            Matricula esperando = guardar(
                    construirBase(usuarioId, curso, res, TipoParticipante.ASISTENTE, false)
                            .estado(EstadoMatricula.EN_ESPERA)
                            .posicionEspera(matriculaRepository.maxPosicionEspera(cursoId) + 1)
                            .build(),
                    "matricula en lista de espera");
            return toResponse(esperando);
        }

        // R-INS-07: curso gratis → ACTIVA directo
        if (!esPago) {
            Matricula m = guardar(construirBase(usuarioId, curso, res, TipoParticipante.ASISTENTE, false)
                    .estado(EstadoMatricula.ACTIVA).build(), "matricula gratuita activada");
            notificar(m, curso, "CURSO_INSCRIPCION_OK");
            notificarEscarapelaSiAplica(m, curso);
            return toResponse(m);
        }

        // R-INS-06: curso pago — crear o reutilizar pago (monto = precio de la modalidad)
        PagoResumen pago = resolverPago(usuarioId, curso, req, res.precioCompra);

        EstadoMatricula estado = pago.aprobado()
                ? EstadoMatricula.ACTIVA
                : EstadoMatricula.PENDIENTE_PAGO;

        Matricula m = guardar(construirBase(usuarioId, curso, res, TipoParticipante.ASISTENTE, false)
                .estado(estado)
                .pagoId(pago.pagoId())
                .reservaExpira(estado == EstadoMatricula.PENDIENTE_PAGO
                        ? LocalDateTime.now().plusHours(reservaHoras)
                        : null)
                .build(), "matricula con pago " + pago.estado());

        notificar(m, curso, estado == EstadoMatricula.ACTIVA
                ? "CURSO_INSCRIPCION_OK"
                : "CURSO_INSCRIPCION_PENDIENTE_PAGO");
        if (estado == EstadoMatricula.ACTIVA) notificarEscarapelaSiAplica(m, curso);
        return toResponse(m);
    }

    /**
     * Resuelve la modalidad de compra, su precio y el set final de acceso
     * (aplicando el bonus de GRABADO si convive con vivo).
     */
    private ModalidadResuelta resolverModalidad(Curso curso, InscribirRequest req) {
        ModalidadTipo tmp = req.getModalidad();
        if (tmp == null && req.getModalidades() != null && !req.getModalidades().isEmpty()) {
            tmp = req.getModalidades().iterator().next(); // back-compat
        }
        if (tmp == null) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Debes elegir una modalidad para inscribirte");
        }
        final ModalidadTipo elegida = tmp;

        CursoModalidad cm = curso.getModalidades().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()) && m.getTipo() == elegida)
                .findFirst()
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.MODALIDAD_NO_DISPONIBLE,
                        "Modalidad " + elegida + " no está activa en el curso"));

        boolean hayVivo = curso.getModalidades().stream()
                .anyMatch(m -> Boolean.TRUE.equals(m.getActivo())
                        && (m.getTipo() == ModalidadTipo.VIRTUAL_LIVE
                         || m.getTipo() == ModalidadTipo.PRESENCIAL));

        // No se puede comprar GRABADO si convive con vivo — se otorga como bonus
        if (cm.getTipo() == ModalidadTipo.GRABADO && hayVivo) {
            throw new CursosApiException(ServerApiStatusCode.MODALIDAD_NO_DISPONIBLE,
                    "GRABADO va incluido con las modalidades en vivo, no se compra por separado");
        }

        Set<ModalidadTipo> acceso = new HashSet<>();
        acceso.add(cm.getTipo());
        // Bonus: si compró vivo y el curso ofrece GRABADO activo, se agrega
        if (cm.getTipo() != ModalidadTipo.GRABADO) {
            boolean grabadoActivo = curso.getModalidades().stream()
                    .anyMatch(m -> Boolean.TRUE.equals(m.getActivo())
                            && m.getTipo() == ModalidadTipo.GRABADO);
            if (grabadoActivo) acceso.add(ModalidadTipo.GRABADO);
        }
        return new ModalidadResuelta(cm.getTipo(), cm.getPrecioCop(), acceso);
    }

    private record ModalidadResuelta(
            ModalidadTipo modalidadCompra,
            BigDecimal precioCompra,
            Set<ModalidadTipo> modalidadesAcceso) {}

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

        // Cortesía no compra: se aceptan las modalidades pedidas tal cual (con
        // bonus implícito si convive con vivo; no hay precio congelado).
        ModalidadResuelta res = resolverCortesia(curso, req.getModalidades());

        Matricula m = guardar(construirBase(req.getUsuarioId(), curso, res, req.getTipo(), true)
                .estado(EstadoMatricula.ACTIVA).build(),
                "cortesia tipo=" + req.getTipo() + " por admin=" + adminUsuarioId);

        notificar(m, curso, "CURSO_CORTESIA");
        return toResponse(m);
    }

    private ModalidadResuelta resolverCortesia(Curso curso, Set<ModalidadTipo> pedidas) {
        Set<ModalidadTipo> acceso = new HashSet<>(pedidas);
        boolean pidioVivo = pedidas.contains(ModalidadTipo.VIRTUAL_LIVE)
                         || pedidas.contains(ModalidadTipo.PRESENCIAL);
        boolean grabadoActivo = curso.getModalidades().stream()
                .anyMatch(m -> Boolean.TRUE.equals(m.getActivo())
                        && m.getTipo() == ModalidadTipo.GRABADO);
        if (pidioVivo && grabadoActivo) acceso.add(ModalidadTipo.GRABADO);
        ModalidadTipo compra = pidioVivo
                ? (pedidas.contains(ModalidadTipo.PRESENCIAL) ? ModalidadTipo.PRESENCIAL : ModalidadTipo.VIRTUAL_LIVE)
                : pedidas.iterator().next();
        return new ModalidadResuelta(compra, null, acceso);
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
            notificarEscarapelaSiAplica(m, m.getCurso());
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
     * Llama a dev-ms-pagos para obtener (o iniciar) el pago. El {@code monto}
     * viene ya calculado del precio de la modalidad seleccionada — pagos NO
     * debe recalcularlo del curso porque el precio se congela aquí.
     */
    private PagoResumen resolverPago(Long usuarioId, Curso curso, InscribirRequest req, BigDecimal monto) {
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
            body.put("montoCurso", monto == null ? 0 : monto.intValue());
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
            notificarEscarapelaSiAplica(siguiente, curso);
        }
        siguiente.setPosicionEspera(null);
        matriculaRepository.save(siguiente);
        log.info("Matrícula {} promovida desde lista de espera del curso {}",
                siguiente.getId(), curso.getId());
    }

    private Matricula.MatriculaBuilder construirBase(Long usuarioId, Curso curso,
                                                      ModalidadResuelta res,
                                                      TipoParticipante tipo,
                                                      boolean cortesia) {
        Matricula.MatriculaBuilder b = Matricula.builder()
                .usuarioId(usuarioId)
                .curso(curso)
                .tipo(tipo)
                .cortesia(cortesia)
                .modalidades(new HashSet<>(res.modalidadesAcceso()))
                .precioCopPagado(cortesia ? null : res.precioCompra());

        // Token de escarapela — solo si la matrícula incluye PRESENCIAL en el
        // acceso resuelto. La URL pública se serviría en /escarapela/{token}
        // aunque el curso no tenga plantilla configurada; el render devolverá
        // un lienzo mínimo en ese caso.
        if (res.modalidadesAcceso().contains(ModalidadTipo.PRESENCIAL)) {
            b.escarapelaToken(generarEscarapelaToken());
        }
        return b;
    }

    private static String generarEscarapelaToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private Matricula guardar(Matricula m, String mensajeLog) {
        Matricula saved = matriculaRepository.save(m);
        log.info("Matrícula {} guardada: {}", saved.getId(), mensajeLog);
        return saved;
    }

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrlBase;

    /**
     * Envía el email ESCARAPELA_LISTA cuando la matrícula acaba de quedar
     * ACTIVA y tiene token generado (matrícula PRESENCIAL). Silencioso si no
     * aplica o si sendmail falla — no bloquea el flujo.
     */
    private void notificarEscarapelaSiAplica(Matricula m, Curso curso) {
        if (m.getEscarapelaToken() == null || m.getEscarapelaToken().isBlank()) return;
        if (m.getEstado() != EstadoMatricula.ACTIVA) return;
        try {
            UsuarioDestinatario dest = resolverDestinatario(m.getUsuarioId());
            if (dest.correo == null || dest.correo.isBlank()) return;

            String sede = curso.getModalidades().stream()
                    .filter(cm -> Boolean.TRUE.equals(cm.getActivo())
                            && cm.getTipo() == ModalidadTipo.PRESENCIAL)
                    .map(CursoModalidad::getSede)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst().orElse("");
            String urlEscarapela = frontendUrlBase.replaceAll("/+$", "")
                    + "/escarapela/" + m.getEscarapelaToken();

            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", "ESCARAPELA_LISTA",
                    "destinatario", Map.of(
                            "correo", dest.correo,
                            "nombre", dest.nombre == null ? "" : dest.nombre),
                    "variables", Map.of(
                            "nombreDestinatario", dest.nombre == null ? "" : dest.nombre,
                            "nombreCurso", curso.getNombre(),
                            "sede", sede,
                            "fechaInicio", curso.getFechaInicio() == null
                                    ? "" : curso.getFechaInicio().toString(),
                            "urlEscarapela", urlEscarapela)
            ));
        } catch (Exception ex) {
            log.warn("No se pudo enviar ESCARAPELA_LISTA para matrícula {}: {}",
                    m.getId(), ex.getMessage());
        }
    }

    private void notificar(Matricula m, Curso curso, String template) {
        try {
            UsuarioDestinatario dest = resolverDestinatario(m.getUsuarioId());
            if (dest.correo == null || dest.correo.isBlank()) {
                log.warn("No se pudo notificar matrícula {} — usuario {} sin email",
                        m.getId(), m.getUsuarioId());
                return;
            }
            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", template,
                    "destinatario", Map.of(
                            "correo", dest.correo,
                            "nombre", dest.nombre == null ? "" : dest.nombre),
                    "variables", Map.of(
                            "nombreDestinatario", dest.nombre == null ? "" : dest.nombre,
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

    /** DTO interno con los datos mínimos del destinatario. */
    private record UsuarioDestinatario(String correo, String nombre) {}

    /**
     * Resuelve el email + nombre real del usuario vía Feign a authenticate.
     * Si el lookup falla, devuelve datos vacíos y se omite el envío.
     */
    @SuppressWarnings("unchecked")
    private UsuarioDestinatario resolverDestinatario(Long usuarioId) {
        try {
            Map<String, Object> resp = authClient.lookup(List.of(usuarioId));
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> u) {
                Map<String, Object> mu = (Map<String, Object>) u;
                String email = mu.get("email") == null ? null : mu.get("email").toString();
                String nombres = mu.get("nombres") == null ? "" : mu.get("nombres").toString();
                String apellidos = mu.get("apellidos") == null ? "" : mu.get("apellidos").toString();
                String nombreCompleto = (nombres + " " + apellidos).trim();
                return new UsuarioDestinatario(email, nombreCompleto.isEmpty() ? nombres : nombreCompleto);
            }
        } catch (Exception ex) {
            log.warn("No se pudo hacer lookup del usuario {}: {}", usuarioId, ex.getMessage());
        }
        return new UsuarioDestinatario(null, null);
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
                .precioCopPagado(m.getPrecioCopPagado())
                .escarapelaToken(m.getEscarapelaToken())
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
