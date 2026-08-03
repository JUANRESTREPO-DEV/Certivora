package com.eduessence.cursos.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Reporte de certificados emitidos por curso. */
@Data
@Builder
public class ReporteCertificadosResponse {

    private List<Fila> filas;
    private long totalEmitidos;
    private long totalPendientes;

    @Data
    @Builder
    public static class Fila {
        private Long cursoId;
        private String nombre;
        private String slug;
        private LocalDateTime fechaFin;

        /** Cuántos certificados ya se emitieron para este curso. */
        private long emitidos;

        /** Cuántos matriculados aún no han recibido certificado. */
        private long pendientes;

        /** Matriculados con acceso "certificado" activo. */
        private long totalMatriculados;

        /** 0..100. */
        private double porcentajeEmision;
    }
}
