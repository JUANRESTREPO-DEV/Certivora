package com.eduessence.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Contrato con dev-ms-sendmail.
 *
 * Body esperado por {@code /internal/enviar-email}:
 * <pre>
 * {
 *   "nombreTemplate": "WELCOME",
 *   "asunto": "Bienvenido",                        // opcional
 *   "destinatario": { "correo": "x@y.com", "nombre": "Juan" },
 *   "variables": { ... }                            // libre
 * }
 * </pre>
 */
@FeignClient(name = "sendmail-service", path = "/sendmail", fallback = SendmailServiceFallback.class)
public interface SendmailServiceClient {

    @PostMapping("/internal/enviar-email")
    void enviarEmail(@RequestBody Map<String, Object> payload);
}
