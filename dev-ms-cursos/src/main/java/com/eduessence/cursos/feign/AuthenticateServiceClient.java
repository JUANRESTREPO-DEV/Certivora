package com.eduessence.cursos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Cliente Feign hacia dev-ms-authenticate para resolver datos de usuarios
 * (instructores, asignados, etc.).
 *
 * El endpoint /api/usuarios/lookup requiere JWT; por ahora el gateway
 * inyecta los headers internos y este micro confía en ellos.
 *
 * Para que la llamada interna no pase por el gateway con JWT, podríamos
 * crear un endpoint /internal/usuarios/lookup. Por simplicidad lo dejamos
 * en el path público con el assumption de que sólo se llama desde el
 * back-end (red interna).
 */
@FeignClient(name = "authenticate-service", path = "/auth",
        fallback = AuthenticateServiceFallback.class)
public interface AuthenticateServiceClient {

    /**
     * Devuelve la lista de usuarios cuyos ids se pasen. Respuesta:
     * <pre>
     *   { statusCode, response: [{ id, nombres, apellidos, email, ... }] }
     * </pre>
     */
    @GetMapping("/api/usuarios/lookup")
    Map<String, Object> lookup(@RequestParam("ids") List<Long> ids);
}
