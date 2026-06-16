package com.eduessence.streaming.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "cursos-service", path = "/cursos", fallback = CursosServiceFallback.class)
public interface CursosServiceClient {

    /** Envía agregado de presencia tras terminar la sesión virtual. */
    @PostMapping("/internal/asistencia-virtual/heartbeat")
    void enviarHeartbeat(@RequestBody Map<String, Object> payload);
}
