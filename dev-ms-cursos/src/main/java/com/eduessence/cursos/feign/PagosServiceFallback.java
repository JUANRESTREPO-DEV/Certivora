package com.eduessence.cursos.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PagosServiceFallback implements PagosServiceClient {

    @Override
    public Map<String, Object> obtenerPago(Long pagoId) {
        log.warn("[Fallback] pagos-service no disponible (pagoId={})", pagoId);
        return Map.of("estado", "DESCONOCIDO");
    }

    @Override
    public Map<String, Object> iniciarPago(Map<String, Object> body) {
        log.warn("[Fallback] pagos-service no disponible (iniciarPago body={})", body);
        return Map.of("error", "PAGOS_NO_DISPONIBLE");
    }

    @Override
    public Map<String, Object> ingresosPorCurso() {
        log.warn("[Fallback] pagos-service no disponible (ingresosPorCurso)");
        return Map.of("response", Map.of());
    }
}
