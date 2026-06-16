package com.eduessence.sendmail.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request del endpoint de preview. El admin puede sobreescribir el HTML
 * (para probar cambios antes de guardarlos en S3) y las variables que
 * normalmente iría a inyectar el sistema en producción.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewRequest {

    /**
     * HTML override. Si viene, se renderiza este HTML en vez de descargar el
     * actual desde S3. Útil para preview en vivo desde el editor del front.
     */
    private String htmlOverride;

    /** Si viene, se usa como asunto previsto (informativo, no se renderiza). */
    private String asunto;

    /** Destinatario simulado (nombre/correo) para los placeholders. */
    private DestinatarioDTO destinatario;

    /** Variables a inyectar al contexto Velocity. Pueden faltar — se rellenan con defaults. */
    private Map<String, Object> variables;
}
