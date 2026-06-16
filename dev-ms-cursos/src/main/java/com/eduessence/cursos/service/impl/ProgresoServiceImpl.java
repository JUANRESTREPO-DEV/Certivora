package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.ActualizarProgresoRequest;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.model.entity.ConfiguracionAprobacion;
import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.entity.ProgresoLeccion;
import com.eduessence.cursos.model.enums.EstadoLeccion;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.repository.LeccionRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.repository.ProgresoLeccionRepository;
import com.eduessence.cursos.service.AprobacionService;
import com.eduessence.cursos.service.MatriculaService;
import com.eduessence.cursos.service.ProgresoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgresoServiceImpl implements ProgresoService {

    private final ProgresoLeccionRepository progresoRepository;
    private final MatriculaRepository matriculaRepository;
    private final LeccionRepository leccionRepository;
    private final MatriculaService matriculaService;
    private final AprobacionService aprobacionService;

    @Override
    @Transactional
    public MatriculaResponse actualizar(Long matriculaId, Long leccionId, ActualizarProgresoRequest req) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.MATRICULA_NO_ENCONTRADA));

        ProgresoLeccion p = progresoRepository.findByMatriculaIdAndLeccionId(matriculaId, leccionId)
                .orElseGet(() -> ProgresoLeccion.builder()
                        .matriculaId(matriculaId).leccionId(leccionId)
                        .estado(EstadoLeccion.NO_INICIADA).segundosVistos(0)
                        .build());

        if (req.getSegundosVistos() != null && req.getSegundosVistos() > p.getSegundosVistos()) {
            p.setSegundosVistos(req.getSegundosVistos());
            if (p.getEstado() == EstadoLeccion.NO_INICIADA) p.setEstado(EstadoLeccion.EN_PROGRESO);
        }
        if (Boolean.TRUE.equals(req.getCompletada())) {
            p.setEstado(EstadoLeccion.COMPLETADA);
            p.setFechaCompletada(LocalDateTime.now());
        }
        progresoRepository.save(p);

        // Recalcular % global
        boolean cumpleUmbralAprobacion = false;
        long total = leccionRepository.countByCursoId(matricula.getCurso().getId());
        if (total > 0) {
            long completadas = progresoRepository.countByMatriculaIdAndEstado(matriculaId, EstadoLeccion.COMPLETADA);
            BigDecimal pct = BigDecimal.valueOf(completadas)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            matricula.setProgresoPct(pct);
            matriculaRepository.save(matricula);

            // ¿Acaba de alcanzar el umbral mínimo de progreso del curso?
            ConfiguracionAprobacion cfg = matricula.getCurso().getConfiguracionAprobacion();
            BigDecimal umbral = cfg != null && cfg.getProgresoMinimoPct() != null
                    ? cfg.getProgresoMinimoPct()
                    : BigDecimal.valueOf(100);
            cumpleUmbralAprobacion = pct.compareTo(umbral) >= 0
                    && matricula.getEstado() != EstadoMatricula.APROBADA;
        }

        // Si llegó al umbral y aún no está APROBADA, dispara la evaluación
        // automática. AprobacionService internamente:
        //   1) checa progreso + notas + presencia
        //   2) si cumple TODO, marca APROBADA + emite el certificado via Feign
        //   3) si falta algo (ej. promedio de exámenes), no cambia el estado
        // → idempotente y seguro de llamar cada vez que se completa una lección
        if (cumpleUmbralAprobacion) {
            try {
                aprobacionService.evaluar(matriculaId);
                log.info("Evaluación automática disparada para matrícula {} al alcanzar el umbral de progreso",
                        matriculaId);
            } catch (Exception ex) {
                // No rompemos el flujo de progreso si la evaluación falla
                log.warn("Evaluación automática falló para matrícula {}: {}",
                        matriculaId, ex.getMessage());
            }
        }
        return matriculaService.obtener(matriculaId);
    }

    @Override
    @Transactional
    public void recalcularProgresoCurso(Long cursoId) {
        long total = leccionRepository.countByCursoId(cursoId);
        List<Matricula> matriculas = matriculaRepository.findAllByCurso_Id(cursoId);
        if (matriculas.isEmpty()) return;

        for (Matricula m : matriculas) {
            BigDecimal pct;
            if (total == 0) {
                pct = BigDecimal.ZERO;
            } else {
                long completadas = progresoRepository.countByMatriculaIdAndEstado(
                        m.getId(), EstadoLeccion.COMPLETADA);
                // El alumno pudo tener lecciones completadas que ya no existen
                // — el count solo refleja progresoLeccion vivas asociadas a la
                // matrícula, no las lecciones borradas, así que el resultado
                // queda acotado a [0, total].
                if (completadas > total) completadas = total;
                pct = BigDecimal.valueOf(completadas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            }
            m.setProgresoPct(pct);
        }
        matriculaRepository.saveAll(matriculas);
        log.info("Progreso recalculado para curso {} ({} matrículas, total lecciones={})",
                cursoId, matriculas.size(), total);
    }
}
