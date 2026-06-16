package com.eduessence.streaming.service.impl;

import com.eduessence.streaming.model.dto.response.StreamCredentialsResponse;
import com.eduessence.streaming.model.dto.response.StreamSessionResponse;
import com.eduessence.streaming.model.entity.StreamSession;
import com.eduessence.streaming.model.entity.ViewerLog;
import com.eduessence.streaming.model.enums.EstadoStream;
import com.eduessence.streaming.model.enums.EventoViewer;
import com.eduessence.streaming.repository.StreamSessionRepository;
import com.eduessence.streaming.repository.ViewerLogRepository;
import com.eduessence.streaming.service.StreamingService;
import com.eduessence.streaming.service.provider.StreamProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingServiceImpl implements StreamingService {

    private final StreamSessionRepository sessionRepository;
    private final ViewerLogRepository viewerLogRepository;
    private final StreamProvider provider;

    @Override
    @Transactional
    public StreamCredentialsResponse crearSesion(Long cursoId, Long sesionVirtualId,
                                                 Long instructorId, String titulo) {
        StreamProvider.StreamCredentials creds = provider.crear(titulo);
        StreamSession sesion = StreamSession.builder()
                .cursoId(cursoId)
                .sesionVirtualId(sesionVirtualId)
                .instructorUsuarioId(instructorId)
                .titulo(titulo)
                .provider(provider.getTipo())
                .providerResourceId(creds.providerResourceId())
                .ingestUrl(creds.ingestUrl())
                .streamKey(creds.streamKey())
                .playbackUrl(creds.playbackUrl())
                .estado(EstadoStream.CREADO)
                .build();
        sesion = sessionRepository.save(sesion);
        log.info("Sesión {} creada con provider {} (resource {})",
                sesion.getId(), provider.getTipo(), creds.providerResourceId());
        return StreamCredentialsResponse.builder()
                .sessionId(sesion.getId())
                .provider(sesion.getProvider())
                .ingestUrl(sesion.getIngestUrl())
                .streamKey(sesion.getStreamKey())
                .playbackUrl(sesion.getPlaybackUrl())
                .build();
    }

    @Override
    @Transactional
    public StreamCredentialsResponse regenerarCredenciales(Long sessionId) {
        StreamSession s = find(sessionId);
        if (s.getEstado() == EstadoStream.EN_VIVO) {
            throw new RuntimeException(
                "No se pueden regenerar credenciales mientras la sesión está EN_VIVO");
        }

        // Liberar recurso anterior (best-effort — no fallar si el provider ya no lo tiene)
        try {
            if (s.getProviderResourceId() != null) {
                provider.eliminar(s.getProviderResourceId());
            }
        } catch (Exception ex) {
            log.warn("No se pudo eliminar recurso anterior del provider ({}): {}",
                    s.getProviderResourceId(), ex.getMessage());
        }

        StreamProvider.StreamCredentials creds = provider.crear(s.getTitulo());
        s.setProvider(provider.getTipo());
        s.setProviderResourceId(creds.providerResourceId());
        s.setIngestUrl(creds.ingestUrl());
        s.setStreamKey(creds.streamKey());
        s.setPlaybackUrl(creds.playbackUrl());
        s.setEstado(EstadoStream.CREADO);
        s.setFechaInicio(null);
        s.setFechaFin(null);
        s.setDuracionRealSegundos(null);
        s = sessionRepository.save(s);

        log.info("Credenciales regeneradas para sesión {} con provider {} (resource {})",
                s.getId(), provider.getTipo(), creds.providerResourceId());

        return StreamCredentialsResponse.builder()
                .sessionId(s.getId())
                .provider(s.getProvider())
                .ingestUrl(s.getIngestUrl())
                .streamKey(s.getStreamKey())
                .playbackUrl(s.getPlaybackUrl())
                .build();
    }

    @Override
    @Transactional
    public StreamSessionResponse marcarEnVivo(Long sessionId) {
        StreamSession s = find(sessionId);
        s.setEstado(EstadoStream.EN_VIVO);
        if (s.getFechaInicio() == null) s.setFechaInicio(LocalDateTime.now());
        provider.iniciar(s.getProviderResourceId());
        return toResponse(sessionRepository.save(s));
    }

    @Override
    @Transactional
    public StreamSessionResponse terminar(Long sessionId) {
        StreamSession s = find(sessionId);
        boolean yaTerminado = s.getEstado() == EstadoStream.TERMINADO;

        s.setEstado(EstadoStream.TERMINADO);
        if (s.getFechaFin() == null) s.setFechaFin(LocalDateTime.now());
        if (s.getFechaInicio() != null) {
            s.setDuracionRealSegundos(Duration.between(s.getFechaInicio(), s.getFechaFin()).toSeconds());
        }
        String recording = provider.terminar(s.getProviderResourceId());
        if (recording != null) s.setRecordingUrl(recording);

        StreamSession saved = sessionRepository.save(s);

        // Libera el recurso en el provider (canal IVS, etc.) para no acumular
        // recursos vivos. Solo si no estaba ya terminado (idempotencia) y si
        // todavía hay providerResourceId que liberar.
        if (!yaTerminado && saved.getProviderResourceId() != null) {
            try {
                provider.eliminar(saved.getProviderResourceId());
                saved.setProviderResourceId(null);
                saved = sessionRepository.save(saved);
                log.info("Recurso del provider liberado al terminar sesión {}", saved.getId());
            } catch (Exception ex) {
                log.warn("No se pudo liberar recurso del provider al terminar sesión {}: {}",
                        saved.getId(), ex.getMessage());
            }
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void eliminar(Long sessionId) {
        StreamSession s = find(sessionId);
        if (s.getEstado() == EstadoStream.EN_VIVO) {
            throw new RuntimeException(
                "No se puede eliminar la sesión mientras está EN_VIVO. Termínala primero.");
        }
        if (s.getProviderResourceId() != null) {
            try {
                provider.eliminar(s.getProviderResourceId());
            } catch (Exception ex) {
                log.warn("No se pudo eliminar recurso del provider {} para sesión {}: {}",
                        s.getProviderResourceId(), sessionId, ex.getMessage());
            }
        }
        sessionRepository.delete(s);
        log.info("Sesión de streaming {} eliminada", sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public StreamSessionResponse obtener(Long sessionId) {
        return toResponse(find(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public StreamSessionResponse obtenerPorStreamKey(String streamKey) {
        return toResponse(sessionRepository.findByStreamKey(streamKey)
                .orElseThrow(() -> new RuntimeException("Stream key no existe")));
    }

    @Override
    @Transactional
    public void registrarEvento(Long sessionId, Long usuarioId, String sessionToken,
                                EventoViewer evento, Integer segundosAcumulados,
                                String ip, String userAgent) {
        viewerLogRepository.save(ViewerLog.builder()
                .streamSessionId(sessionId).usuarioId(usuarioId).sessionToken(sessionToken)
                .evento(evento).segundosAcumulados(segundosAcumulados)
                .ip(ip).userAgent(userAgent).build());
        if (evento == EventoViewer.JOIN) {
            StreamSession s = find(sessionId);
            long actuales = viewerLogRepository.countByStreamSessionIdAndEvento(sessionId, EventoViewer.JOIN)
                    - viewerLogRepository.countByStreamSessionIdAndEvento(sessionId, EventoViewer.LEAVE);
            if (actuales > s.getViewersPeak()) {
                s.setViewersPeak((int) actuales);
                sessionRepository.save(s);
            }
        }
    }

    /* ─────────────────────── helpers ─────────────────────── */

    private StreamSession find(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada: " + id));
    }

    private StreamSessionResponse toResponse(StreamSession s) {
        return StreamSessionResponse.builder()
                .id(s.getId()).cursoId(s.getCursoId()).sesionVirtualId(s.getSesionVirtualId())
                .instructorUsuarioId(s.getInstructorUsuarioId()).titulo(s.getTitulo())
                .provider(s.getProvider()).estado(s.getEstado())
                .playbackUrl(s.getPlaybackUrl()).recordingUrl(s.getRecordingUrl())
                .fechaInicio(s.getFechaInicio()).fechaFin(s.getFechaFin())
                .duracionRealSegundos(s.getDuracionRealSegundos())
                .viewersPeak(s.getViewersPeak())
                .build();
    }
}
