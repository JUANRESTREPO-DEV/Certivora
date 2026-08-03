package com.eduessence.pagos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Cliente Feign hacia dev-ms-authenticate para resolver datos de usuarios
 * (email real y nombre completo) al notificar por correo.
 */
@FeignClient(name = "authenticate-service", path = "/auth",
        fallback = AuthenticateServiceFallback.class)
public interface AuthenticateServiceClient {

    /**
     * Devuelve la lista de usuarios cuyos ids se pasen.
     * Respuesta: {@code { statusCode, response: [{ id, nombres, apellidos, email, ... }] }}
     */
    @GetMapping("/api/usuarios/lookup")
    Map<String, Object> lookup(@RequestParam("ids") List<Long> ids);
}
