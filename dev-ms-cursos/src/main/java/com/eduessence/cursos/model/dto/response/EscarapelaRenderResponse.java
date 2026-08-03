package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload público que devuelve el endpoint {@code /public/escarapela/{token}}.
 * Incluye la configuración visual del template + los datos del asistente
 * ya resueltos, listos para render en el front (página pública o PDF).
 *
 * <p>El campo {@code qrUrl} es la URL a la que apuntará el QR generado en
 * el front. En fase 1 apunta a la misma página pública; en fase 2 (sponsors)
 * apuntará al endpoint de captura de lead.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscarapelaRenderResponse {
    private String token;
    private String fondoUrl;
    private String posicionesJson;

    // Datos del asistente resueltos
    private String nombreAsistente;
    private String emailAsistente;
    private String telefonoAsistente;

    // Datos del evento
    private Long cursoId;
    private String nombreCurso;
    private String cursoSlug;
    private String sede;
    private String fechaInicio;

    /** URL del QR — se usa dentro del componente <img /> del front. */
    private String qrUrl;
}
