package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Reglas de aprobación de un curso. Todos los campos son opcionales en el
 * update — el null = no tocar, mantén el valor actual.
 */
@Data
public class ActualizarConfigAprobacionRequest {

    /** Nota mínima general para exámenes y actividades (0-100). */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal notaMinimaGeneral;

    /** % mínimo de avance de lecciones (modalidad GRABADO). */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal progresoMinimoPct;

    /** % mínimo de conectividad / log para modalidad VIRTUAL_LIVE. */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal presenciaMinimaPct;

    /** % mínimo de check-in QR para modalidad PRESENCIAL. */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal presenciaQrMinimaPct;
}
