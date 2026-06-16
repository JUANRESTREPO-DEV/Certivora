package com.eduessence.sendmail.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Resultado de previsualizar un template. Devuelve el HTML renderizado y un
 * snapshot del contexto efectivo (variables que se inyectaron) para que el
 * admin las vea en la UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponse {

    /** HTML resultante tras renderizar Velocity con el contexto efectivo. */
    private String htmlRender;

    /** Asunto que se enviaría (override del request o asunto default del template). */
    private String asunto;

    /** Contexto efectivo: variables que terminaron usándose en el render. */
    private Map<String, Object> contexto;

    /** Variables requeridas por el template que NO vinieron en el request. */
    private List<String> variablesFaltantes;
}
