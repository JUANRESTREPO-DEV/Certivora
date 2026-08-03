package com.eduessence.auth.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CursosServiceFallback implements CursosServiceClient {
    @Override
    public Map<String, Object> matriculasPorUsuario(Long usuarioId) {
        log.warn("[Fallback] cursos-service no disponible (matriculasPorUsuario {})", usuarioId);
        return Map.of("response", List.of());
    }
}
