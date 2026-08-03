package com.eduessence.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Cliente hacia dev-ms-cursos — solo endpoints internos para reportar
 * el historial de matrículas del usuario en el back-office.
 */
@FeignClient(name = "cursos-service", path = "/cursos", fallback = CursosServiceFallback.class)
public interface CursosServiceClient {

    /** Matrículas del usuario (con curso + estado + fecha). */
    @GetMapping("/internal/usuarios/{usuarioId}/matriculas")
    Map<String, Object> matriculasPorUsuario(@PathVariable("usuarioId") Long usuarioId);
}
