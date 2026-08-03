package com.eduessence.pagos.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CursosServiceFallback implements CursosServiceClient {

    @Override
    public void activarMatricula(Long matriculaId) {
        log.warn("[Fallback] cursos-service no disponible (matriculaId={})", matriculaId);
    }

    @Override
    public void notificarPagoAprobado(Long pagoId) {
        log.warn("[Fallback] cursos-service no disponible (notificarPagoAprobado pagoId={})", pagoId);
    }

    @Override
    public Map<String, Object> cursoBasico(Long cursoId) {
        log.warn("[Fallback] cursos-service no disponible (cursoBasico cursoId={})", cursoId);
        return Map.of("statusCode", 503, "response", Map.of());
    }
}
