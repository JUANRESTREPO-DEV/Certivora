package com.eduessence.pagos.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
}
