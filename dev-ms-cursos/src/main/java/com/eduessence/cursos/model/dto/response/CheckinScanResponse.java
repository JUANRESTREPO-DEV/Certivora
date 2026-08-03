package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resultado del scan del QR. El front muestra el nombre + una animación
 * de éxito, o mensaje de error específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinScanResponse {
    /** OK / YA_REGISTRADO / MATRICULA_NO_ENCONTRADA / SESION_NO_ENCONTRADA / SESION_CANCELADA. */
    private String estado;
    private String mensaje;

    private Long matriculaId;
    private Long sesionId;
    private String nombreCompleto;
    private String email;
    private LocalDateTime fechaCheckin;
    private long totalCheckinSesion;
}
