package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.TipoPregunta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado completo de un intento de examen. Polimórfico según fase:
 *  - Mientras está activo: {@code preguntas} contiene preguntas SIN respuestas
 *    correctas (para que el alumno responda) y {@code respuestas} lleva lo
 *    que ya guardó.
 *  - Tras finalizar: {@code respuestas} incluye corrección, puntaje y
 *    {@code aprobado}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentoExamenResponse {

    private Long id;
    private Long matriculaId;
    private Long examenId;
    private String examenTitulo;
    private Integer duracionMinutos;
    private Integer numeroIntento;
    private Integer intentosMaximos;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private BigDecimal puntaje;
    private BigDecimal notaMinima;
    private Boolean finalizado;
    private Boolean aprobado;
    private Boolean requiereCalificacionManual;

    /** Preguntas del examen — sin respuesta correcta cuando finalizado=false. */
    private List<PreguntaIntentoDTO> preguntas;

    /** Respuestas guardadas — sin corrección cuando finalizado=false. */
    private List<RespuestaIntentoDTO> respuestas;

    /* ─── DTOs internos ─── */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreguntaIntentoDTO {
        private Long id;
        private String enunciado;
        private TipoPregunta tipo;
        private Integer orden;
        private BigDecimal puntos;
        private List<OpcionIntentoDTO> opciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpcionIntentoDTO {
        private Long id;
        private String texto;
        private Integer orden;
        /** {@code true} solo cuando el intento está finalizado. */
        private Boolean correcta;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RespuestaIntentoDTO {
        private Long preguntaId;
        /** IDs de opciones seleccionadas por el alumno. */
        private List<Long> opcionesSeleccionadas;
        /** Para TEXTO_CORTO. */
        private String respuestaTexto;
        /** Solo poblado tras finalizar. */
        private BigDecimal puntosObtenidos;
        /** Solo poblado tras finalizar. */
        private Boolean correcta;
    }
}
