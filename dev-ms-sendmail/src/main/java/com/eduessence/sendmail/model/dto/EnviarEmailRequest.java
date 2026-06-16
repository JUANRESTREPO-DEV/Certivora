package com.eduessence.sendmail.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request al endpoint {@code /internal/enviar-email}.
 *
 * Body de ejemplo:
 * <pre>
 * {
 *   "nombreTemplate": "WELCOME",
 *   "asunto": "Bienvenido a Eduessence",     // opcional → usa asunto_default
 *   "destinatario": { "correo": "x@y.com", "nombre": "Juan" },
 *   "variables": { "ciudad": "Barranquilla", "curso": "Pie diabético" }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnviarEmailRequest {

    @NotBlank
    private String nombreTemplate;

    /** Opcional: si no viene se usa {@code asunto_default} del template. */
    private String asunto;

    @NotNull
    @Valid
    private DestinatarioDTO destinatario;

    /** Mapa libre de variables que se inyectan al contexto Velocity. */
    private Map<String, Object> variables;
}
