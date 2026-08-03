package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila del listado de asistentes en el panel de check-in. Incluye datos
 * personales para búsqueda manual + estado de asistencia.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsistenteCheckinResponse {
    private Long matriculaId;
    private Long usuarioId;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private String documento;
    /** Token de escarapela — permite mostrar link a su badge desde el panel. */
    private String escarapelaToken;

    /** True si ya hizo check-in a esta sesión. */
    private Boolean checkinHecho;
    private LocalDateTime fechaCheckin;
}
