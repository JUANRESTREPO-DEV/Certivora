package com.eduessence.cursos.controller;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.HeartbeatAgregadoRequest;
import com.eduessence.cursos.model.dto.response.AprobacionResultDTO;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.model.entity.AsistenciaVirtual;
import com.eduessence.cursos.model.entity.SesionVirtual;
import com.eduessence.cursos.repository.AsistenciaVirtualRepository;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.repository.SesionVirtualRepository;
import com.eduessence.cursos.service.AprobacionService;
import com.eduessence.cursos.service.MatriculaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "cursos-service";

    private final MatriculaService matriculaService;
    private final AprobacionService aprobacionService;
    private final AsistenciaVirtualRepository asistenciaRepository;
    private final SesionVirtualRepository sesionVirtualRepository;
    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;

    @PostMapping("/matriculas/{id}/activar")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ok(matriculaService.activar(id), "Matrícula activada"));
    }

    /**
     * Webhook desde dev-ms-pagos cuando un pago se aprueba. Pagos no conoce
     * el id de la matrícula — solo el {@code pagoId}. Aquí lo resolvemos por
     * el campo {@code pago_id} que cursos guardó al inscribir.
     */
    @PostMapping("/pagos/{pagoId}/aprobado")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> pagoAprobado(@PathVariable Long pagoId) {
        var m = matriculaRepository.findByPagoId(pagoId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.MATRICULA_NO_ENCONTRADA,
                        "Sin matrícula vinculada al pago " + pagoId));
        return ResponseEntity.ok(ok(matriculaService.activar(m.getId()),
                "Matrícula activada por pago aprobado"));
    }

    @GetMapping("/matriculas/{id}")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(matriculaService.obtener(id), "OK"));
    }

    /**
     * Datos básicos de un curso para uso interno (ej. notificaciones desde
     * pagos/certificados). Solo devuelve id + nombre + slug — no exposición
     * completa del curso.
     */
    @GetMapping("/cursos/{id}/basico")
    public ResponseEntity<GeneralResponseDTO<java.util.Map<String, Object>>> cursoBasico(@PathVariable Long id) {
        return cursoRepository.findById(id)
                .map(c -> ResponseEntity.ok(ok(
                        java.util.Map.<String, Object>of(
                                "id", c.getId(),
                                "nombre", c.getNombre() == null ? "" : c.getNombre(),
                                "slug", c.getSlug() == null ? "" : c.getSlug()
                        ),
                        "OK")))
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO));
    }

    @GetMapping("/matriculas/{id}/evaluar")
    public ResponseEntity<GeneralResponseDTO<AprobacionResultDTO>> evaluar(@PathVariable Long id) {
        return ResponseEntity.ok(ok(aprobacionService.evaluar(id), "OK"));
    }

    /** Todas las matrículas de un usuario — usado por el panel /app/usuarios. */
    @GetMapping("/usuarios/{usuarioId}/matriculas")
    public ResponseEntity<GeneralResponseDTO<java.util.List<MatriculaResponse>>> matriculasPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(ok(matriculaService.misMatriculas(usuarioId), "OK"));
    }

    @PostMapping("/asistencia-virtual/heartbeat")
    public ResponseEntity<GeneralResponseDTO<Void>> heartbeat(@Valid @RequestBody HeartbeatAgregadoRequest req) {
        SesionVirtual sv = sesionVirtualRepository.findById(req.getSesionVirtualId())
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Sesión virtual no existe"));

        AsistenciaVirtual av = asistenciaRepository
                .findByMatriculaIdAndSesionVirtualId(req.getMatriculaId(), req.getSesionVirtualId())
                .orElseGet(() -> AsistenciaVirtual.builder()
                        .matriculaId(req.getMatriculaId())
                        .sesionVirtualId(req.getSesionVirtualId())
                        .minutosConectado(0).cumpleMinimo(false)
                        .primeraConexion(LocalDateTime.now())
                        .build());

        av.setMinutosConectado(av.getMinutosConectado() + req.getMinutosConectado());
        av.setUltimaConexion(LocalDateTime.now());

        if (sv.getDuracionMinutos() != null && sv.getDuracionMinutos() > 0) {
            BigDecimal pct = BigDecimal.valueOf(av.getMinutosConectado())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(sv.getDuracionMinutos()), 2, RoundingMode.HALF_UP);
            av.setPorcentajePresencia(pct);
            av.setCumpleMinimo(pct.compareTo(new BigDecimal("70.00")) >= 0);
        }
        asistenciaRepository.save(av);
        return ResponseEntity.ok(ok(null, "Heartbeat registrado"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
