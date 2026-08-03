package com.eduessence.auth.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CertificadosServiceFallback implements CertificadosServiceClient {
    @Override
    public Map<String, Object> certificadosPorUsuario(Long usuarioId) {
        log.warn("[Fallback] certificados-service no disponible (certificadosPorUsuario {})", usuarioId);
        return Map.of("response", List.of());
    }
}
