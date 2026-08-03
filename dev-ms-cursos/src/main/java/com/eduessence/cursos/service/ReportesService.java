package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.response.ReporteAsistenciaCursoResponse;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaDetalleResponse;
import com.eduessence.cursos.model.dto.response.ReporteCertificadosResponse;
import com.eduessence.cursos.model.dto.response.ReporteMatriculasResponse;

import java.util.List;

/**
 * Reportes ejecutivos del back-office para la sección Eventos → Reportes.
 * Todos los métodos son de sólo lectura y agregan datos de múltiples
 * micros (matrículas + pagos + certificados) vía Feign.
 */
public interface ReportesService {

    /** Lista resumen de cursos con al menos una sesión PRESENCIAL. */
    List<ReporteAsistenciaCursoResponse> asistenciaCursos();

    /** Detalle: sesiones + matriz asistente × sesión de un curso. */
    ReporteAsistenciaDetalleResponse asistenciaDetalle(Long cursoId);

    /** Matrículas por curso + desglose por modalidad + ingresos. */
    ReporteMatriculasResponse matriculas();

    /** Certificados emitidos + pendientes por curso. */
    ReporteCertificadosResponse certificados();
}
