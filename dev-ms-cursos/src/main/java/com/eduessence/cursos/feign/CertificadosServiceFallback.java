package com.eduessence.cursos.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CertificadosServiceFallback implements CertificadosServiceClient {
    @Override
    public Map<String, Object> emitir(Map<String, Object> payload) {
        log.warn("[Fallback] certificados-service no disponible. Payload: {}", payload);
        return Map.of();
    }

    @Override
    public Map<String, Object> listarTemplates(String q, Long cursoId) {
        log.warn("[Fallback] certificados-service no disponible (listar templates)");
        return Map.of("response", java.util.List.of());
    }

    @Override
    public Map<String, Object> obtenerTemplate(Long id) {
        log.warn("[Fallback] certificados-service no disponible (obtener template {})", id);
        return Map.of();
    }

    @Override
    public Map<String, Object> crearTemplate(Map<String, Object> payload) {
        log.warn("[Fallback] certificados-service no disponible (crear template). Payload: {}", payload);
        return Map.of();
    }

    @Override
    public Map<String, Object> clonarTemplate(Long id, Map<String, Object> payload) {
        log.warn("[Fallback] certificados-service no disponible (clonar template {}). Payload: {}", id, payload);
        return Map.of();
    }

    @Override
    public Map<String, Object> actualizarTemplate(Long id, Map<String, Object> payload) {
        log.warn("[Fallback] certificados-service no disponible (actualizar template {}). Payload: {}", id, payload);
        return Map.of();
    }

    @Override
    public Map<String, Object> borrarTemplate(Long id) {
        log.warn("[Fallback] certificados-service no disponible (borrar template {})", id);
        return Map.of();
    }

    @Override
    public Map<String, Object> emitidosPorCurso() {
        log.warn("[Fallback] certificados-service no disponible (emitidosPorCurso)");
        return Map.of("response", Map.of());
    }
}
