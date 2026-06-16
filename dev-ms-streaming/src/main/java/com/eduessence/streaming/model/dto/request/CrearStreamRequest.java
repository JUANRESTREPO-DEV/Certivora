package com.eduessence.streaming.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearStreamRequest {

    @NotNull
    private Long cursoId;

    private Long sesionVirtualId;

    @NotBlank
    private String titulo;

    private Integer duracionEstimadaMinutos;
}
