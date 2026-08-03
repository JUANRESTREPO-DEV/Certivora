package com.eduessence.cursos.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Detalle de asistencia de un curso: sesiones + matriz asistente × sesión. */
@Data
@Builder
public class ReporteAsistenciaDetalleResponse {
    private Long cursoId;
    private String nombre;
    private String sede;

    /** Sesiones ordenadas cronológicamente. */
    private List<SesionResumen> sesiones;

    /** Un item por matriculado. */
    private List<AsistenteReporte> asistentes;

    @Data
    @Builder
    public static class SesionResumen {
        private Long sesionId;
        private String titulo;
        private LocalDateTime fechaInicio;
        private Integer duracionMinutos;
        private Boolean cancelada;
        private long checkins;
    }

    @Data
    @Builder
    public static class AsistenteReporte {
        private Long matriculaId;
        private Long usuarioId;
        private String nombreCompleto;
        private String email;
        private String documento;

        /** sesionId -> fecha check-in (null = no vino). */
        private Map<Long, LocalDateTime> checkinsPorSesion;

        private int totalCheckins;
        private double porcentajeAsistencia; // 0..100
    }
}
