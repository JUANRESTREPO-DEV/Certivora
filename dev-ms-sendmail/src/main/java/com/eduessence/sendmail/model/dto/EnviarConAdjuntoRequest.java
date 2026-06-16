package com.eduessence.sendmail.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request para envío masivo a varios destinatarios. Puede usar template o
 * cuerpoHtml libre. Acepta adjuntos en base64.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnviarConAdjuntoRequest {

    /** Opcional: si se especifica, render dinámico. */
    private String nombreTemplate;

    private String asunto;

    /** Si no se usa template, este HTML se envía tal cual. */
    private String cuerpoHtml;

    @NotEmpty
    @Valid
    @NotNull
    private List<DestinatarioDTO> destinatarios;

    private Map<String, Object> variables;

    @Valid
    private List<AdjuntoDTO> adjuntos;
}
