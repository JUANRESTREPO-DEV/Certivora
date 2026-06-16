package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.ModalidadTipo;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/**
 * Inscripción del flujo público — siempre se persiste con
 * {@code tipo = ASISTENTE}. Para otros tipos (PONENTE / ORGANIZADOR / STAFF)
 * el admin usa {@link CortesiaRequest}.
 */
@Data
public class InscribirRequest {

    /** Modalidades elegidas. Deben ser un subset de las activas del curso. */
    @NotEmpty
    private Set<ModalidadTipo> modalidades;

    /**
     * Si el usuario ya inició el pago en pasos previos y vuelve con su
     * {@code pagoId}, se reutiliza. Si llega {@code null} y el curso es pago,
     * cursos-service crea el pago vía Feign a dev-ms-pagos.
     */
    private Long pagoId;

    /**
     * Cupón a aplicar al crear el pago. Solo se usa cuando {@code pagoId} es
     * {@code null}. Si el cupón cubre 100% del precio, dev-ms-pagos devuelve
     * {@code GRATIS_POR_CUPON} y la matrícula entra directo en {@code ACTIVA}.
     */
    private String cuponCodigo;
}
