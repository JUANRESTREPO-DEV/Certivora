package com.eduessence.cursos.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class StreamingServiceFallback implements StreamingServiceClient {
    @Override
    public Map<String, Object> crearStream(Map<String, Object> payload) {
        log.warn("[Fallback] streaming-service no disponible");
        return Map.of();
    }

    @Override
    public Map<String, Object> regenerarStream(String sessionId) {
        log.warn("[Fallback] streaming-service no disponible para regenerar {}", sessionId);
        return Map.of();
    }

    @Override
    public Map<String, Object> terminarStream(String sessionId) {
        log.warn("[Fallback] streaming-service no disponible para terminar {}", sessionId);
        return Map.of();
    }

    @Override
    public Map<String, Object> eliminarStream(String sessionId) {
        log.warn("[Fallback] streaming-service no disponible para eliminar {}", sessionId);
        return Map.of();
    }
}
