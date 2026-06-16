package com.eduessence.pagos.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class IniciarPagoRequest {

    @NotNull
    private Long cursoId;

    /** Monto del curso. Pagos no lo calcula (lo trae cursos). */
    @NotNull
    @Positive
    private Integer montoCurso;

    /** Opcional. Si viene, se aplica. */
    private String cuponCodigo;
}
