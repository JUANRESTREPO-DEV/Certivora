package com.eduessence.streaming.service;

import com.eduessence.streaming.feign.CursosServiceClient;
import com.eduessence.streaming.model.entity.StreamSession;
import com.eduessence.streaming.model.entity.ViewerLog;
import com.eduessence.streaming.model.enums.EstadoStream;
import com.eduessence.streaming.model.enums.EventoViewer;
import com.eduessence.streaming.repository.StreamSessionRepository;
import com.eduessence.streaming.repository.ViewerLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuando una sesión pasa a TERMINADO, agrega los heartbeats por viewer y
 * envía los minutos conectados al dev-ms-cursos para que actualice
 * {@code asistencia_virtual.minutos_conectado}.
 *
 * Si una sesión virtual del curso tiene {@code matriculaId} asociado, el
 * cliente debe enviarlo en el header del heartbeat (futuro). Por ahora
 * asumimos {@code usuarioId == matriculaId}, lo cual sirve para v1; cuando
 * se conecten de verdad, mapear vía Feign cursos.findMatriculaPorUsuarioYCurso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatAggregatorService {

    private final StreamSessionRepository sessionRepository;
    private final ViewerLogRepository viewerLogRepository;
    private final CursosServiceClient cursosClient;

    /** Cada 5 minutos revisa sesiones recién terminadas y agrega presencia. */
    @Scheduled(fixedDelay = 5 * 60_000L)
    @Transactional
    public void procesar() {
        List<StreamSession> terminadas = sessionRepository.findAllByEstado(EstadoStream.TERMINADO);
        for (StreamSession s : terminadas) {
            if (s.getSesionVirtualId() == null) continue;
            try {
                agregarYEnviar(s);
            } catch (Exception ex) {
                log.warn("Falló agregación sesión {}: {}", s.getId(), ex.getMessage());
            }
        }
    }

    private void agregarYEnviar(StreamSession s) {
        // Para cada usuario distinto en viewer_log, calcular minutos conectado
        Map<Long, Integer> minutosPorUsuario = new HashMap<>();

        // Tomamos todos los viewer_log del session ordenados por timestamp
        viewerLogRepository.findAll().stream()
                .filter(v -> v.getStreamSessionId().equals(s.getId()))
                .forEach(v -> {
                    if (v.getEvento() == EventoViewer.HEARTBEAT && v.getSegundosAcumulados() != null) {
                        int minutos = v.getSegundosAcumulados() / 60;
                        minutosPorUsuario.merge(v.getUsuarioId(), minutos, Math::max);
                    }
                });

        for (Map.Entry<Long, Integer> e : minutosPorUsuario.entrySet()) {
            try {
                cursosClient.enviarHeartbeat(Map.of(
                        "matriculaId", e.getKey(),
                        "sesionVirtualId", s.getSesionVirtualId(),
                        "minutosConectado", e.getValue()
                ));
            } catch (Exception ex) {
                log.warn("No se pudo enviar heartbeat a cursos para usuario {}: {}",
                        e.getKey(), ex.getMessage());
            }
        }
    }
}
