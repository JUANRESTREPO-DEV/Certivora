package com.eduessence.sendmail.model.dto;

import com.eduessence.sendmail.model.enums.EstadoEnvio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Espejo de la entidad {@code EmailEnvio} para la bandeja admin del front.
 * El campo {@code variables} viene como mapa parseado del JSON guardado para
 * que la UI lo muestre como tabla en el detalle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEnvioResponse {
    private Long id;

    private Long templateId;
    private String templateNombre;

    private String destinatario;
    private String nombreDestinatario;
    private String asunto;

    private EstadoEnvio estado;

    /** Variables Velocity reales con que se intentó renderizar. */
    private Map<String, Object> variables;

    /** Si FALLIDO o REINTENTAR, traza del error. */
    private String mensajeError;

    private LocalDateTime fechaEnvio;
}
