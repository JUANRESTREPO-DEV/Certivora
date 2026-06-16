package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ActualizarProgresoRequest {

    @PositiveOrZero
    private Integer segundosVistos;

    private Boolean completada;
}
