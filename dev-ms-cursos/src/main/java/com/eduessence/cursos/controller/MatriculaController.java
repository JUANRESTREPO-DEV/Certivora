package com.eduessence.cursos.controller;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CancelarMatriculaRequest;
import com.eduessence.cursos.model.dto.request.InscribirRequest;
import com.eduessence.cursos.model.dto.response.AprobacionResultDTO;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.service.AprobacionService;
import com.eduessence.cursos.service.MatriculaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de matrícula para usuarios autenticados (no admin).
 *
 * <p>El gateway inyecta {@code X-User-Id} y {@code X-User-Roles} en cada
 * request; aquí los leemos para identidad. La autorización de "este usuario
 * solo opera sobre matrículas que le pertenecen" se hace dentro de cada
 * handler.</p>
 */
@Tag(name = "Matrículas")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatriculaController {

    private static final String SERVICE = "cursos-service";
    private final MatriculaService matriculaService;
    private final AprobacionService aprobacionService;

    /* ─── inscribir flujo público ─── */

    @PostMapping("/cursos/{cursoId}/inscripciones")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> inscribir(
            @PathVariable Long cursoId,
            @Valid @RequestBody InscribirRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse data = matriculaService.inscribir(userId, cursoId, body);
        return ResponseEntity.status(201).body(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(201).serviceName(SERVICE)
                .message(mensajePorEstado(data))
                .response(data).build());
    }

    /* ─── cancelar (usuario sobre su propia matrícula) ─── */

    @PostMapping("/matriculas/{id}/cancelar")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> cancelar(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid CancelarMatriculaRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse actual = matriculaService.obtener(id);
        if (!userId.equals(actual.getUsuarioId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Solo el dueño de la matrícula puede cancelar (usa /admin para cancelar a otros)");
        }
        MatriculaResponse data = matriculaService.cancelar(id, userId, body);
        return ResponseEntity.ok(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(200).serviceName(SERVICE)
                .message("Matrícula cancelada")
                .response(data).build());
    }

    /* ─── evaluar / pedir certificado ─── */

    /**
     * El alumno dispara la evaluación de su matrícula para que se calcule
     * progreso/notas/presencia y, si cumple los criterios, pase a APROBADA y
     * se emita el certificado. Idempotente: si ya está APROBADA + emitido,
     * solo devuelve el resultado actual.
     */
    @PostMapping("/matriculas/{id}/evaluar")
    public ResponseEntity<GeneralResponseDTO<AprobacionResultDTO>> evaluar(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse actual = matriculaService.obtener(id);
        if (!userId.equals(actual.getUsuarioId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Solo el dueño de la matrícula puede solicitar su evaluación");
        }
        AprobacionResultDTO data = aprobacionService.evaluar(id);
        return ResponseEntity.ok(GeneralResponseDTO.<AprobacionResultDTO>builder()
                .statusCode(200).serviceName(SERVICE)
                .message(data.isAprobado() ? "Matrícula evaluada · aprobada" : "Aún no cumple los criterios")
                .response(data).build());
    }

    /* ─── consultas ─── */

    @GetMapping("/matriculas/me")
    public ResponseEntity<GeneralResponseDTO<List<MatriculaResponse>>> misMatriculas(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = ControllerSecurity.parseUserId(userIdHeader);
        return ResponseEntity.ok(GeneralResponseDTO.<List<MatriculaResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(matriculaService.misMatriculas(userId)).build());
    }

    @GetMapping("/matriculas/{id}")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> obtener(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        Long userId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse data = matriculaService.obtener(id);
        // user solo puede ver las suyas — admin puede ver cualquiera
        if (!userId.equals(data.getUsuarioId()) && !ControllerSecurity.esAdmin(rolesHeader)) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "No tienes permiso para ver esta matrícula");
        }
        return ResponseEntity.ok(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(data).build());
    }

    /* ─── helpers ─── */

    private static String mensajePorEstado(MatriculaResponse m) {
        return switch (m.getEstado()) {
            case ACTIVA           -> "Inscripción confirmada";
            case PENDIENTE_PAGO   -> "Reserva creada — confirma el pago antes de "
                                    + (m.getReservaExpira() == null ? "la fecha límite" : m.getReservaExpira());
            case EN_ESPERA        -> "Cupo lleno — entraste a la lista de espera (posición "
                                    + m.getPosicionEspera() + ")";
            default               -> "Inscripción registrada";
        };
    }
}
