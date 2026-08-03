package com.eduessence.auth.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PagosServiceFallback implements PagosServiceClient {
    @Override
    public Map<String, Object> pagosPorUsuario(Long usuarioId) {
        log.warn("[Fallback] pagos-service no disponible (pagosPorUsuario {})", usuarioId);
        return Map.of("response", List.of());
    }
}
