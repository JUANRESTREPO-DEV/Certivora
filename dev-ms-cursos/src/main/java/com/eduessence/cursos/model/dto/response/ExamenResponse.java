package com.eduessence.cursos.model.dto.response;

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
public class ExamenResponse {
    private Long id;
    private Long cursoId;
    private String titulo;
    private String descripcion;
    private Integer duracionMinutos;
    private Integer intentosMaximos;
    private BigDecimal notaMinimaPropia;
    private Boolean shufflePreguntas;
    private Boolean shuffleOpciones;
    private List<PreguntaResponse> preguntas;
}
