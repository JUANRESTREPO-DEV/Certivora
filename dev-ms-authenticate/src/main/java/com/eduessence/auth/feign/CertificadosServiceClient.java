package com.eduessence.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "certificados-service", path = "/certificados", fallback = CertificadosServiceFallback.class)
public interface CertificadosServiceClient {

    @GetMapping("/internal/certificados/usuarios/{usuarioId}/certificados")
    Map<String, Object> certificadosPorUsuario(@PathVariable("usuarioId") Long usuarioId);
}
