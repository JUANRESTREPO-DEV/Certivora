package com.eduessence.cursos.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Reporte de matrículas por curso — con desglose por modalidad + ingresos. */
@Data
@Builder
public class ReporteMatriculasResponse {

    private List<Fila> filas;

    /** Totales del período. */
    private long totalMatriculas;
    private long totalPresencial;
    private long totalVirtualLive;
    private long totalGrabado;
    private BigDecimal ingresosTotales;

    @Data
    @Builder
    public static class Fila {
        private Long cursoId;
        private String nombre;
        private String slug;
        private LocalDateTime fechaInicio;

        private long matriculas;
        private long presencial;
        private long virtualLive;
        private long grabado;

        private BigDecimal ingresos;

        /** Estado del curso (BORRADOR/PUBLICADO/ARCHIVADO). */
        private String estado;
    }
}
