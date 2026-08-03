package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Devuelve la plantilla de escarapela cuando el admin la consulta o
 * edita. No incluye variables resueltas — sólo la configuración visual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscarapelaTemplateResponse {
    private Long id;
    private Long cursoId;
    private String fondoUrl;
    private String posicionesJson;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
