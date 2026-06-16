package com.eduessence.sendmail.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Envío de prueba para un template desde la UI admin. Renderiza con las
 * variables provistas y manda un correo real al destinatario indicado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSendRequest {

    /** HTML override (opcional). Si viene, sobrescribe el de S3 para esta prueba. */
    private String htmlOverride;

    /** Asunto opcional — si no viene se usa el asunto_default del template. */
    private String asunto;

    @NotNull
    @Valid
    private DestinatarioDTO destinatario;

    /** Variables Velocity para el render. */
    private Map<String, Object> variables;
}
