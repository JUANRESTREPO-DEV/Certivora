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
public class AprobacionResultDTO {
    private Long matriculaId;
    private boolean aprobado;

    private BigDecimal progresoPct;
    private BigDecimal progresoMinimoRequerido;
    private boolean cumpleProgreso;

    private BigDecimal promedioExamenes;
    private BigDecimal promedioActividades;
    private BigDecimal notaMinimaRequerida;
    private boolean cumpleNotas;

    private BigDecimal porcentajePresencia;
    private BigDecimal presenciaMinimaRequerida;
    private boolean cumplePresencia;
    private boolean aplicaPresencia;

    private List<String> motivosNoAprueba;
}
