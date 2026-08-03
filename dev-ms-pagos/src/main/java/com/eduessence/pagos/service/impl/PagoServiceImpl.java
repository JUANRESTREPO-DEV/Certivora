package com.eduessence.pagos.service.impl;

import com.eduessence.pagos.exception.PagosApiException;
import com.eduessence.pagos.exception.ServerApiStatusCode;
import com.eduessence.pagos.feign.AuthenticateServiceClient;
import com.eduessence.pagos.feign.CursosServiceClient;
import com.eduessence.pagos.feign.SendmailServiceClient;
import com.eduessence.pagos.model.dto.request.ConfirmarPagoRequest;
import com.eduessence.pagos.model.dto.request.IniciarPagoRequest;
import com.eduessence.pagos.model.dto.response.IniciarPagoResponse;
import com.eduessence.pagos.model.dto.response.PagoResponse;
import com.eduessence.pagos.model.dto.response.ValidarCuponResponse;
import com.eduessence.pagos.model.entity.Cupon;
import com.eduessence.pagos.model.entity.Pago;
import com.eduessence.pagos.model.enums.EstadoPago;
import com.eduessence.pagos.model.enums.MetodoPago;
import com.eduessence.pagos.repository.PagoRepository;
import com.eduessence.pagos.service.ComprobanteStorageService;
import com.eduessence.pagos.service.CuponService;
import com.eduessence.pagos.service.PagoService;
import com.eduessence.pagos.utils.LlaveGenerador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Flujo:
 *  - iniciar:
 *      · valida cupón si viene → reserva (usos++)
 *      · si monto_final == 0 → estado GRATIS_POR_CUPON + activa matrícula
 *      · si > 0 → genera llave Bre-B + reserva_expira = now+48h
 *  - aprobar: estado=APROBADO + Feign cursos.activarMatricula + email
 *  - rechazar: estado=RECHAZADO + libera cupón
 *  - expirarVencidos: cron job
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CuponService cuponService;
    private final LlaveGenerador llaveGenerador;
    private final CursosServiceClient cursosClient;
    private final SendmailServiceClient sendmail;
    private final ComprobanteStorageService comprobanteStorage;
    private final AuthenticateServiceClient authClient;

    @Value("${eduessence.pagos.reserva-horas-default:48}")
    private int reservaHorasDefault;

    @Override
    @Transactional
    public IniciarPagoResponse iniciar(Long usuarioId, IniciarPagoRequest req) {
        int montoOriginal = req.getMontoCurso();
        int montoDescuento = 0;
        Cupon cuponAplicado = null;

        if (req.getCuponCodigo() != null && !req.getCuponCodigo().isBlank()) {
            ValidarCuponResponse v = cuponService.validar(req.getCuponCodigo(), usuarioId, req.getCursoId(), montoOriginal);
            montoDescuento = v.getMontoDescuento();
        }
        int montoFinal = Math.max(0, montoOriginal - montoDescuento);
        boolean gratis = montoFinal == 0;

        Pago pago = Pago.builder()
                .usuarioId(usuarioId).cursoId(req.getCursoId())
                .montoOriginal(montoOriginal)
                .montoDescuento(montoDescuento)
                .montoCop(montoFinal)
                .moneda("COP")
                .metodo(MetodoPago.LLAVE_BREB)
                .estado(gratis ? EstadoPago.GRATIS_POR_CUPON : EstadoPago.PENDIENTE_LLAVE)
                .llave(gratis ? null : llaveGenerador.generar())
                .reservaExpira(gratis ? null : LocalDateTime.now().plusHours(reservaHorasDefault))
                .build();
        pago = pagoRepository.save(pago);

        if (req.getCuponCodigo() != null && !req.getCuponCodigo().isBlank()) {
            cuponAplicado = cuponService.reservar(req.getCuponCodigo(), usuarioId,
                    req.getCursoId(), pago.getId(), montoDescuento);
            pago.setCuponId(cuponAplicado.getId());
            pagoRepository.save(pago);
        }

        if (gratis) {
            pago.setFechaAprobacion(LocalDateTime.now());
            pagoRepository.save(pago);
            // El curso activa matrícula tras crear el pago; aquí simulamos
            log.info("Pago {} gratis por cupón {}", pago.getId(), cuponAplicado == null ? "?" : cuponAplicado.getCodigo());
        } else {
            // Notificar al usuario la llave
            UsuarioDestinatario dest = resolverDestinatario(usuarioId);
            if (dest.correo != null && !dest.correo.isBlank()) {
                try {
                    sendmail.enviarEmail(Map.of(
                            "nombreTemplate", "PAGO_LLAVE_PENDIENTE",
                            "destinatario", Map.of(
                                    "correo", dest.correo,
                                    "nombre", dest.nombre == null ? "" : dest.nombre),
                            "variables", Map.of(
                                    "nombreDestinatario", dest.nombre == null ? "" : dest.nombre,
                                    "llave", pago.getLlave(),
                                    "monto", "$" + String.format("%,d", montoFinal),
                                    "horasReserva", reservaHorasDefault,
                                    "nombreCurso", nombreDelCurso(req.getCursoId())
                            )
                    ));
                } catch (Exception ex) {
                    log.warn("No se pudo enviar email de llave: {}", ex.getMessage());
                }
            }
        }

        return IniciarPagoResponse.builder()
                .pagoId(pago.getId()).estado(pago.getEstado())
                .montoOriginal(montoOriginal).montoDescuento(montoDescuento).montoCop(montoFinal)
                .llave(pago.getLlave()).reservaExpira(pago.getReservaExpira())
                .cursoGratis(gratis)
                .build();
    }

    @Override
    @Transactional
    public PagoResponse confirmarPago(Long pagoId, ConfirmarPagoRequest req) {
        Pago p = find(pagoId);
        if (p.getEstado() != EstadoPago.PENDIENTE_LLAVE) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El pago no está en PENDIENTE_LLAVE");
        }
        p.setEstado(EstadoPago.PENDIENTE_CONFIRMACION);
        p.setComprobanteUrl(req.getComprobanteUrl());
        p.setObservacion(req.getObservacion());
        return toResponse(pagoRepository.save(p));
    }

    @Override
    @Transactional
    public PagoResponse aprobar(Long pagoId, Long adminId, String observacion) {
        Pago p = find(pagoId);
        p.setEstado(EstadoPago.APROBADO);
        p.setAprobadoPorUsuarioId(adminId);
        p.setFechaAprobacion(LocalDateTime.now());
        if (observacion != null) p.setObservacion(observacion);
        p = pagoRepository.save(p);

        // Activar matrícula vinculada — cursos la resuelve por pago_id
        try {
            cursosClient.notificarPagoAprobado(pagoId);
            log.info("Pago {} aprobado → cursos notificado para activar matrícula", pagoId);
        } catch (Exception ex) {
            log.warn("Notificación a cursos falló para pago {}: {}", pagoId, ex.getMessage());
        }

        // Email confirmación con destinatario real
        UsuarioDestinatario destAprob = resolverDestinatario(p.getUsuarioId());
        if (destAprob.correo != null && !destAprob.correo.isBlank()) {
            try {
                sendmail.enviarEmail(Map.of(
                        "nombreTemplate", "PAGO_APROBADO",
                        "destinatario", Map.of(
                                "correo", destAprob.correo,
                                "nombre", destAprob.nombre == null ? "" : destAprob.nombre),
                        "variables", Map.of(
                                "nombreDestinatario", destAprob.nombre == null ? "" : destAprob.nombre,
                                "nombreCurso", nombreDelCurso(p.getCursoId()),
                                "numeroReferencia", p.getLlave() == null ? "" : p.getLlave(),
                                "fechaInicio", ""
                        )
                ));
            } catch (Exception ex) {
                log.warn("No se pudo enviar email de aprobación: {}", ex.getMessage());
            }
        }
        return toResponse(p);
    }

    @Override
    @Transactional
    public PagoResponse rechazar(Long pagoId, Long adminId, String observacion) {
        Pago p = find(pagoId);
        p.setEstado(EstadoPago.RECHAZADO);
        p.setAprobadoPorUsuarioId(adminId);
        if (observacion != null) p.setObservacion(observacion);
        cuponService.liberar(pagoId);
        Pago saved = pagoRepository.save(p);

        // Notificar al usuario con el motivo del rechazo
        UsuarioDestinatario dest = resolverDestinatario(p.getUsuarioId());
        if (dest.correo != null && !dest.correo.isBlank()) {
            try {
                sendmail.enviarEmail(Map.of(
                        "nombreTemplate", "PAGO_RECHAZADO",
                        "destinatario", Map.of(
                                "correo", dest.correo,
                                "nombre", dest.nombre == null ? "" : dest.nombre),
                        "variables", Map.of(
                                "nombreDestinatario", dest.nombre == null ? "" : dest.nombre,
                                "nombreCurso", nombreDelCurso(p.getCursoId()),
                                "observacion", observacion == null ? "" : observacion,
                                "referencia", p.getLlave() == null ? "" : p.getLlave()
                        )
                ));
            } catch (Exception ex) {
                log.warn("No se pudo enviar email de rechazo pago {}: {}", pagoId, ex.getMessage());
            }
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public int expirarVencidos() {
        List<Pago> vencidos = pagoRepository.findAllByEstadoAndReservaExpiraBefore(
                EstadoPago.PENDIENTE_LLAVE, LocalDateTime.now());
        int n = 0;
        for (Pago p : vencidos) {
            p.setEstado(EstadoPago.EXPIRADO);
            pagoRepository.save(p);
            cuponService.liberar(p.getId());

            // Notificar al usuario que su reserva expiró
            UsuarioDestinatario dest = resolverDestinatario(p.getUsuarioId());
            if (dest.correo != null && !dest.correo.isBlank()) {
                try {
                    sendmail.enviarEmail(Map.of(
                            "nombreTemplate", "CURSO_INSCRIPCION_EXPIRADA",
                            "destinatario", Map.of(
                                    "correo", dest.correo,
                                    "nombre", dest.nombre == null ? "" : dest.nombre),
                            "variables", Map.of(
                                    "nombreDestinatario", dest.nombre == null ? "" : dest.nombre,
                                    "nombreCurso", nombreDelCurso(p.getCursoId()),
                                    "referencia", p.getLlave() == null ? "" : p.getLlave()
                            )
                    ));
                } catch (Exception ex) {
                    log.warn("No se pudo notificar expiración de pago {}: {}", p.getId(), ex.getMessage());
                }
            }
            n++;
        }
        log.info("Expirados {} pagos vencidos", n);
        return n;
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtener(Long pagoId) {
        return toResponse(find(pagoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> misPagos(Long usuarioId) {
        return pagoRepository.findAllByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PagoResponse subirComprobante(Long pagoId, Long usuarioId,
                                          MultipartFile file, String observacion) {
        Pago p = find(pagoId);
        if (!p.getUsuarioId().equals(usuarioId)) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Solo el dueño del pago puede subir el comprobante");
        }
        if (p.getEstado() != EstadoPago.PENDIENTE_LLAVE
                && p.getEstado() != EstadoPago.PENDIENTE_CONFIRMACION) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El pago no acepta más comprobantes (estado=" + p.getEstado() + ")");
        }

        ComprobanteStorageService.SubidaResult r = comprobanteStorage.subir(pagoId, file);
        p.setComprobanteUrl(r.urlPublica());
        p.setEstado(EstadoPago.PENDIENTE_CONFIRMACION);
        if (observacion != null && !observacion.isBlank()) {
            String prev = p.getObservacion();
            p.setObservacion(prev == null || prev.isBlank()
                    ? observacion
                    : prev + "\n---\n" + observacion);
        }
        p = pagoRepository.save(p);
        log.info("Comprobante subido para pago {} → {} ({} bytes)", pagoId, r.s3Key(), r.bytes());
        return toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPendientesConfirmacion() {
        // Devolvemos tanto los que están esperando comprobante (PENDIENTE_LLAVE)
        // como los que ya lo subieron (PENDIENTE_CONFIRMACION). Esto le da al
        // admin visibilidad completa del pipeline para hacer follow-up o
        // aprobar directamente si tiene visibilidad bancaria.
        //
        // La query es nativa — pasa nombres de enum como String para evitar
        // el problema de Hibernate convirtiendo List<Enum> en native queries.
        return pagoRepository
                .findAllByEstadoInOrderByFechaCreacionAsc(
                        List.of(EstadoPago.PENDIENTE_LLAVE.name(),
                                EstadoPago.PENDIENTE_CONFIRMACION.name()))
                .stream().map(this::toResponse).toList();
    }

    private Pago find(Long pagoId) {
        return pagoRepository.findById(pagoId)
                .orElseThrow(() -> new PagosApiException(ServerApiStatusCode.PAGO_NO_ENCONTRADO));
    }

    private PagoResponse toResponse(Pago p) {
        return PagoResponse.builder()
                .id(p.getId()).usuarioId(p.getUsuarioId()).cursoId(p.getCursoId())
                .montoOriginal(p.getMontoOriginal()).montoDescuento(p.getMontoDescuento())
                .montoCop(p.getMontoCop()).moneda(p.getMoneda())
                .metodo(p.getMetodo()).estado(p.getEstado())
                .llave(p.getLlave()).reservaExpira(p.getReservaExpira())
                .cuponId(p.getCuponId()).comprobanteUrl(p.getComprobanteUrl())
                .observacion(p.getObservacion())
                .fechaCreacion(p.getFechaCreacion()).fechaAprobacion(p.getFechaAprobacion())
                .build();
    }

    /* ─────────────────── Lookup del nombre real del curso ─────────────────── */

    /**
     * Resuelve el nombre del curso vía Feign a cursos. Si falla el lookup
     * o devuelve vacío, cae a {@code "Curso {id}"} como fallback seguro.
     */
    @SuppressWarnings("unchecked")
    private String nombreDelCurso(Long cursoId) {
        try {
            Map<String, Object> resp = cursosClient.cursoBasico(cursoId);
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof Map<?, ?> data) {
                Object nombre = ((Map<String, Object>) data).get("nombre");
                if (nombre != null && !nombre.toString().isBlank()) {
                    return nombre.toString();
                }
            }
        } catch (Exception ex) {
            log.warn("Lookup del curso {} falló: {}", cursoId, ex.getMessage());
        }
        return "Curso " + cursoId;
    }

    /* ─────────────────── Lookup del email real ─────────────────── */

    /** DTO interno con los datos mínimos del destinatario del email. */
    private record UsuarioDestinatario(String correo, String nombre) {}

    /**
     * Resuelve el email + nombre real del usuario vía Feign a authenticate.
     * Si el lookup falla, devuelve datos vacíos y se debe omitir el envío.
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
}
