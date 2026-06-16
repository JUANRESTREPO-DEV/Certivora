package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.StreamingServiceClient;
import com.eduessence.cursos.model.dto.request.CrearSesionRequest;
import com.eduessence.cursos.model.dto.response.SesionResponse;
import com.eduessence.cursos.model.entity.SesionVirtual;
import com.eduessence.cursos.model.enums.TipoSesion;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.SesionVirtualRepository;
import com.eduessence.cursos.service.SesionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesionServiceImpl implements SesionService {

    private final SesionVirtualRepository repo;
    private final CursoRepository cursoRepo;
    private final StreamingServiceClient streamingClient;

    @Value("${eduessence.qr.checkin-base-url:https://eduessence.com/asistencia/check-in}")
    private String qrBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<SesionResponse> listar(Long cursoId) {
        validarCurso(cursoId);
        return repo.findAll().stream()
                .filter(s -> cursoId.equals(s.getCursoId()))
                .sorted(Comparator.comparing(SesionVirtual::getFechaInicio))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SesionResponse crear(Long cursoId, CrearSesionRequest req) {
        validarCurso(cursoId);

        SesionVirtual s = SesionVirtual.builder()
                .cursoId(cursoId)
                .tipo(req.getTipo() == null ? TipoSesion.VIRTUAL : req.getTipo())
                .titulo(req.getTitulo())
                .descripcion(req.getDescripcion())
                .fechaInicio(req.getFechaInicio())
                .duracionMinutos(req.getDuracionMinutos() == null ? 60 : req.getDuracionMinutos())
                .ubicacionTexto(req.getUbicacionTexto())
                .build();

        if (s.getTipo() == TipoSesion.VIRTUAL) {
            // Pedimos un canal a streaming-service; si está caído, persistimos
            // la sesión sin streamId y se podrá completar luego desde admin.
            try {
                Map<String, Object> resp = streamingClient.crearStream(Map.of(
                        "cursoId", cursoId,
                        "titulo", req.getTitulo(),
                        "fechaInicio", req.getFechaInicio() != null ? req.getFechaInicio().toString() : null
                ));
                Object data = resp == null ? null : resp.getOrDefault("response", resp);
                if (data instanceof Map<?, ?> m) {
                    Object sid = m.get("sessionId");
                    Object playback = m.get("playbackUrl");
                    Object key = m.get("streamKey");
                    Object ingest = m.get("ingestUrl");
                    if (sid != null)      s.setStreamSessionId(sid.toString());
                    if (playback != null) s.setPlaybackUrl(playback.toString());
                    if (key != null)      s.setStreamKey(key.toString());
                    if (ingest != null)   s.setIngestUrl(ingest.toString());
                }
            } catch (Exception ex) {
                log.warn("No se pudo crear canal en streaming-service: {}", ex.getMessage());
            }
        } else if (s.getTipo() == TipoSesion.PRESENCIAL) {
            s.setQrToken(generarQrTokenUnico());
        }

        return toDto(repo.save(s));
    }

    @Override
    @Transactional
    public SesionResponse actualizar(Long cursoId, Long sesionId, CrearSesionRequest req) {
        SesionVirtual s = findOrThrow(cursoId, sesionId);
        if (req.getTitulo() != null) s.setTitulo(req.getTitulo());
        if (req.getDescripcion() != null) s.setDescripcion(req.getDescripcion());
        if (req.getFechaInicio() != null) s.setFechaInicio(req.getFechaInicio());
        if (req.getDuracionMinutos() != null) s.setDuracionMinutos(req.getDuracionMinutos());
        if (req.getUbicacionTexto() != null) s.setUbicacionTexto(req.getUbicacionTexto());

        // Cambio de tipo VIRTUAL ↔ PRESENCIAL: efecto colateral
        if (req.getTipo() != null && req.getTipo() != s.getTipo()) {
            TipoSesion previo = s.getTipo();
            s.setTipo(req.getTipo());
            if (req.getTipo() == TipoSesion.PRESENCIAL) {
                if (s.getQrToken() == null || s.getQrToken().isBlank()) {
                    s.setQrToken(generarQrTokenUnico());
                }
                // Si venía de VIRTUAL, libera el canal en el provider y limpia campos
                if (previo == TipoSesion.VIRTUAL) {
                    liberarCanalStreaming(s);
                }
            }
        }
        return toDto(repo.save(s));
    }

    @Override
    @Transactional
    public void borrar(Long cursoId, Long sesionId) {
        SesionVirtual s = findOrThrow(cursoId, sesionId);
        if (s.getTipo() == TipoSesion.VIRTUAL) {
            liberarCanalStreaming(s);
        }
        repo.delete(s);
    }

    /**
     * Pide al streaming-service eliminar el canal del provider (AWS IVS, etc.).
     * Idempotente y tolerante: si el streaming-service está caído o no encuentra
     * el canal, igual seguimos adelante. Limpia los campos cacheados en la entidad.
     */
    private void liberarCanalStreaming(SesionVirtual s) {
        String streamSessionId = s.getStreamSessionId();
        if (streamSessionId != null && !streamSessionId.isBlank()) {
            try {
                streamingClient.eliminarStream(streamSessionId);
                log.info("Canal de streaming {} liberado para sesión {}", streamSessionId, s.getId());
            } catch (Exception ex) {
                log.warn("No se pudo liberar canal de streaming {} para sesión {}: {}",
                        streamSessionId, s.getId(), ex.getMessage());
            }
        }
        s.setStreamSessionId(null);
        s.setStreamKey(null);
        s.setIngestUrl(null);
        s.setPlaybackUrl(null);
    }

    @Override
    @Transactional
    public SesionResponse regenerarCredencialesStream(Long cursoId, Long sesionId) {
        SesionVirtual s = findOrThrow(cursoId, sesionId);
        if (s.getTipo() != TipoSesion.VIRTUAL) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Solo las sesiones VIRTUAL tienen credenciales de streaming");
        }
        if (s.getStreamSessionId() == null || s.getStreamSessionId().isBlank()) {
            // No había stream en el back: lo creamos limpio
            try {
                Map<String, Object> resp = streamingClient.crearStream(Map.of(
                        "cursoId", cursoId,
                        "titulo", s.getTitulo(),
                        "fechaInicio", s.getFechaInicio() != null ? s.getFechaInicio().toString() : null
                ));
                aplicarCredenciales(s, resp);
            } catch (Exception ex) {
                log.warn("No se pudo crear stream para regenerar: {}", ex.getMessage());
                throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "streaming-service no disponible. Intenta de nuevo.");
            }
            return toDto(repo.save(s));
        }

        try {
            Map<String, Object> resp = streamingClient.regenerarStream(s.getStreamSessionId());
            aplicarCredenciales(s, resp);
        } catch (Exception ex) {
            log.warn("No se pudo regenerar credenciales en streaming-service: {}", ex.getMessage());
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "No se pudieron regenerar las credenciales. Intenta de nuevo.");
        }
        // Una sesión recién regenerada no tiene grabación todavía
        s.setRecordingUrl(null);
        return toDto(repo.save(s));
    }

    @Override
    @Transactional
    public SesionResponse terminarTransmision(Long cursoId, Long sesionId) {
        SesionVirtual s = findOrThrow(cursoId, sesionId);
        if (s.getTipo() != TipoSesion.VIRTUAL) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Solo las sesiones VIRTUAL pueden terminarse");
        }
        if (s.getStreamSessionId() == null || s.getStreamSessionId().isBlank()) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Esta sesión no tiene un stream activo");
        }
        try {
            Map<String, Object> resp = streamingClient.terminarStream(s.getStreamSessionId());
            Object data = resp == null ? null : resp.getOrDefault("response", resp);
            if (data instanceof Map<?, ?> m) {
                Object rec = m.get("recordingUrl");
                if (rec != null) s.setRecordingUrl(rec.toString());
            }
        } catch (Exception ex) {
            log.warn("No se pudo terminar el stream {}: {}", s.getStreamSessionId(), ex.getMessage());
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "No pudimos terminar la transmisión. Intenta de nuevo.");
        }
        // Tras terminar, el canal queda liberado en el provider — limpia campos
        // sensibles pero mantén recordingUrl y la entidad para tracking histórico.
        s.setStreamSessionId(null);
        s.setStreamKey(null);
        s.setIngestUrl(null);
        return toDto(repo.save(s));
    }

    @Override
    @Transactional
    public SesionResponse cancelar(Long cursoId, Long sesionId, String razon) {
        SesionVirtual s = findOrThrow(cursoId, sesionId);
        if (Boolean.TRUE.equals(s.getCancelada())) {
            return toDto(s);
        }
        s.setCancelada(true);
        s.setRazonCancelacion(razon == null || razon.isBlank() ? null : razon.trim());
        s.setFechaCancelacion(java.time.LocalDateTime.now());

        // Si es VIRTUAL y tiene canal en streaming-service, libéralo
        if (s.getTipo() == TipoSesion.VIRTUAL) {
            liberarCanalStreaming(s);
        }
        log.info("Sesión {} del curso {} cancelada. Razón: {}", sesionId, cursoId, razon);
        return toDto(repo.save(s));
    }

    /** Vuelca al SesionVirtual los campos devueltos por streaming-service. */
    private void aplicarCredenciales(SesionVirtual s, Map<String, Object> resp) {
        Object data = resp == null ? null : resp.getOrDefault("response", resp);
        if (!(data instanceof Map<?, ?> m)) return;
        Object sid = m.get("sessionId");
        Object playback = m.get("playbackUrl");
        Object key = m.get("streamKey");
        Object ingest = m.get("ingestUrl");
        if (sid != null)      s.setStreamSessionId(sid.toString());
        if (playback != null) s.setPlaybackUrl(playback.toString());
        if (key != null)      s.setStreamKey(key.toString());
        if (ingest != null)   s.setIngestUrl(ingest.toString());
    }

    /* helpers */

    private void validarCurso(Long cursoId) {
        if (!cursoRepo.existsById(cursoId)) {
            throw new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO);
        }
    }

    private SesionVirtual findOrThrow(Long cursoId, Long sesionId) {
        SesionVirtual s = repo.findById(sesionId).orElseThrow(() ->
                new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Sesión no encontrada"));
        if (!cursoId.equals(s.getCursoId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "La sesión no pertenece al curso");
        }
        return s;
    }

    private String generarQrTokenUnico() {
        // 12 chars del UUID base64 — suficiente para evitar colisiones
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private SesionResponse toDto(SesionVirtual s) {
        String checkInUrl = (s.getTipo() == TipoSesion.PRESENCIAL && s.getQrToken() != null)
                ? qrBaseUrl.replaceAll("/$", "") + "/" + s.getQrToken()
                : null;

        return SesionResponse.builder()
                .id(s.getId())
                .cursoId(s.getCursoId())
                .tipo(s.getTipo())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .fechaInicio(s.getFechaInicio())
                .duracionMinutos(s.getDuracionMinutos())
                .streamSessionId(s.getStreamSessionId())
                .playbackUrl(s.getPlaybackUrl())
                .recordingUrl(s.getRecordingUrl())
                .streamKey(s.getStreamKey())
                .ingestUrl(s.getIngestUrl())
                .ubicacionTexto(s.getUbicacionTexto())
                .qrToken(s.getQrToken())
                .qrUrlCheckIn(checkInUrl)
                .cancelada(s.getCancelada())
                .razonCancelacion(s.getRazonCancelacion())
                .fechaCancelacion(s.getFechaCancelacion())
                .build();
    }
}
