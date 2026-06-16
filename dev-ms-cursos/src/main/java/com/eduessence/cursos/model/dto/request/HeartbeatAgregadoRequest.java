package com.eduessence.cursos.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class HeartbeatAgregadoRequest {

    @NotNull
    private Long matriculaId;

    @NotNull
    private Long sesionVirtualId;

    @NotNull
    @PositiveOrZero
    private Integer minutosConectado;
}
