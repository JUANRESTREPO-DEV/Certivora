package com.eduessence.cursos.model.dto;

import com.eduessence.cursos.model.enums.ModalidadTipo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Modalidad de un curso con su precio y configuración específica.
 *
 * Reglas de precio (validadas en el servicio):
 *  - {@code VIRTUAL_LIVE} y {@code PRESENCIAL} activas: precio obligatorio (≥ 0).
 *  - {@code GRABADO} en curso híbrido (con otra modalidad viva activa): precio
 *    DEBE ser {@code null} — el acceso a la grabación es bonus incluido.
 *  - {@code GRABADO} como única modalidad activa: precio obligatorio (≥ 0).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalidadCursoDTO {

    @NotNull
    private ModalidadTipo tipo;

    @PositiveOrZero
    private BigDecimal precioCop;

    /** Solo para PRESENCIAL. */
    @Size(max = 200)
    private String sede;

    /** Cupo específico de esta modalidad. {@code null} = usar cupo global del curso. */
    @PositiveOrZero
    private Integer cupoModalidad;

    /** Por default {@code true} al crear. Permite desactivar sin borrar histórico. */
    private Boolean activo;
}
