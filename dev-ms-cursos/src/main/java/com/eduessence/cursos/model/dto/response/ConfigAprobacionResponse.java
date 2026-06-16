package com.eduessence.cursos.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigAprobacionResponse {
    private Long cursoId;
    private BigDecimal notaMinimaGeneral;
    private BigDecimal progresoMinimoPct;
    private BigDecimal presenciaMinimaPct;
    private BigDecimal presenciaQrMinimaPct;
}
