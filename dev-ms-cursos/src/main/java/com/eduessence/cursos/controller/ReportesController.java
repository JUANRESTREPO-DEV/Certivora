package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaCursoResponse;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaDetalleResponse;
import com.eduessence.cursos.model.dto.response.ReporteCertificadosResponse;
import com.eduessence.cursos.model.dto.response.ReporteMatriculasResponse;
import com.eduessence.cursos.service.ReportesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reportes ejecutivos del back-office. Consumido por el módulo
 * {@code /app/eventos/reportes} del front.
 */
@Tag(name = "Reportes")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReportesController {

    private static final String SERVICE = "cursos-service";
    private final ReportesService reportesService;

    @Operation(summary = "Resumen de asistencia por curso presencial")
    @GetMapping("/asistencia/cursos")
    public ResponseEntity<GeneralResponseDTO<List<ReporteAsistenciaCursoResponse>>> asistenciaCursos() {
        return ResponseEntity.ok(ok(reportesService.asistenciaCursos(), "OK"));
    }

    @Operation(summary = "Detalle de asistencia de un curso (sesiones + asistentes)")
    @GetMapping("/asistencia/cursos/{cursoId}")
    public ResponseEntity<GeneralResponseDTO<ReporteAsistenciaDetalleResponse>> asistenciaDetalle(
            @PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(reportesService.asistenciaDetalle(cursoId), "OK"));
    }

    @Operation(summary = "Matrículas por curso + desglose modalidad + ingresos")
    @GetMapping("/matriculas")
    public ResponseEntity<GeneralResponseDTO<ReporteMatriculasResponse>> matriculas() {
        return ResponseEntity.ok(ok(reportesService.matriculas(), "OK"));
    }

    @Operation(summary = "Certificados emitidos + pendientes por curso")
    @GetMapping("/certificados")
    public ResponseEntity<GeneralResponseDTO<ReporteCertificadosResponse>> certificados() {
        return ResponseEntity.ok(ok(reportesService.certificados(), "OK"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
