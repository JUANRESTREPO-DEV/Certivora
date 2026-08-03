package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila del listado de check-ins existentes de un curso. La ven todos los
 * users (visible en el panel derecho). Ordenado por fecha DESC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinItemResponse {
    private Long asistenciaId;
    private Long matriculaId;
    private Long usuarioId;
    private String nombreCompleto;
    private String email;

    private Long sesionId;
    private String sesionTitulo;
    private LocalDateTime sesionFechaInicio;

    private LocalDateTime fechaCheckin;
}
