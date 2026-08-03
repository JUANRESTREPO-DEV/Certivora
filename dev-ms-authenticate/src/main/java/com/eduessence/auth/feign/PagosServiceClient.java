package com.eduessence.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "pagos-service", path = "/pagos", fallback = PagosServiceFallback.class)
public interface PagosServiceClient {

    @GetMapping("/internal/usuarios/{usuarioId}/pagos")
    Map<String, Object> pagosPorUsuario(@PathVariable("usuarioId") Long usuarioId);
}
