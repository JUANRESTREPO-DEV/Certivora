package com.eduessence.streaming.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CursosServiceFallback implements CursosServiceClient {
    @Override
    public void enviarHeartbeat(Map<String, Object> payload) {
        log.warn("[Fallback] cursos-service no disponible. Payload: {}", payload);
    }
}
