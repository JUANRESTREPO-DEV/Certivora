package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.TipoPregunta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaResponse {
    private Long id;
    private Long examenId;
    private String enunciado;
    private TipoPregunta tipo;
    private Integer orden;
    private BigDecimal puntos;
    private String respuestaEsperada;
    private List<OpcionResponse> opciones;
}
