package com.eduessence.cursos.model.dto.request;

import com.eduessence.cursos.model.enums.TipoPregunta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PreguntaRequest {

    /** ID si es update parcial; null si es nueva. */
    private Long id;

    @NotBlank
    private String enunciado;

    @NotNull
    private TipoPregunta tipo;

    private Integer orden;

    private BigDecimal puntos;

    /** Sólo para TEXTO_CORTO. */
    private String respuestaEsperada;

    @Valid
    private List<OpcionRequest> opciones;
}
