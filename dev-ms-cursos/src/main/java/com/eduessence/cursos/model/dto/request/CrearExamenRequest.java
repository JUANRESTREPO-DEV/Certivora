package com.eduessence.cursos.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CrearExamenRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    private Integer duracionMinutos;

    private Integer intentosMaximos;

    private BigDecimal notaMinimaPropia;

    private Boolean shufflePreguntas;

    private Boolean shuffleOpciones;

    /** Preguntas anidadas — se reemplazan completamente en update. */
    @Valid
    private List<PreguntaRequest> preguntas;
}
