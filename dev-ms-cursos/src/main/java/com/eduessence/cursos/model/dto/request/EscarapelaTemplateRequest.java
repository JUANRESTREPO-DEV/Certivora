package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload para crear o actualizar la plantilla de escarapela de un curso.
 * Todos los campos son opcionales — un {@code fondoUrl} vacío significa
 * "lienzo blanco" (el editor visual del front lo permite).
 */
@Data
public class EscarapelaTemplateRequest {

    @Size(max = 500)
    private String fondoUrl;

    /** JSON serializado con las posiciones/estilos de las variables. */
    private String posicionesJson;

    private Boolean activo;
}
