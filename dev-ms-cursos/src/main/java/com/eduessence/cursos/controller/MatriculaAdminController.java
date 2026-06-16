package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CancelarMatriculaRequest;
import com.eduessence.cursos.model.dto.request.CortesiaRequest;
import com.eduessence.cursos.model.dto.response.AprobacionResultDTO;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.service.AprobacionService;
import com.eduessence.cursos.service.MatriculaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Operaciones administrativas sobre matrículas. Todas requieren rol
 * {@code ADMIN} o {@code GERENTE} (verificado vía {@link ControllerSecurity}).
 */
@Tag(name = "Matrículas — Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class MatriculaAdminController {

    private static final String SERVICE = "cursos-service";
    private final MatriculaService matriculaService;
    private final AprobacionService aprobacionService;

    /* ─── crear cortesía ─── */

    @PostMapping("/cursos/{cursoId}/inscripciones/cortesia")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> crearCortesia(
            @PathVariable Long cursoId,
            @Valid @RequestBody CortesiaRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        ControllerSecurity.requireAdmin(rolesHeader);
        Long adminId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse data = matriculaService.crearCortesia(adminId, cursoId, body);
        return ResponseEntity.status(201).body(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(201).serviceName(SERVICE)
                .message("Cortesía " + data.getTipo() + " otorgada")
                .response(data).build());
    }

    /* ─── cancelar / reembolsar ─── */

    @PostMapping("/matriculas/{id}/cancelar")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> cancelar(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid CancelarMatriculaRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        ControllerSecurity.requireAdmin(rolesHeader);
        Long adminId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse data = matriculaService.cancelar(id, adminId, body);
        return ResponseEntity.ok(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(200).serviceName(SERVICE)
                .message("Matrícula cancelada por admin")
                .response(data).build());
    }

    @PostMapping("/matriculas/{id}/reembolsar")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> reembolsar(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid CancelarMatriculaRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        ControllerSecurity.requireAdmin(rolesHeader);
        Long adminId = ControllerSecurity.parseUserId(userIdHeader);
        MatriculaResponse data = matriculaService.reembolsar(id, adminId, body);
        return ResponseEntity.ok(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(200).serviceName(SERVICE)
                .message("Matrícula marcada como reembolsada")
                .response(data).build());
    }

    /* ─── listado por curso ─── */

    @GetMapping("/cursos/{cursoId}/inscripciones")
    public ResponseEntity<GeneralResponseDTO<List<MatriculaResponse>>> listar(
            @PathVariable Long cursoId,
            @RequestParam(value = "estado", required = false) EstadoMatricula estado,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        ControllerSecurity.requireAdmin(rolesHeader);
        return ResponseEntity.ok(GeneralResponseDTO.<List<MatriculaResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(matriculaService.listarPorCurso(cursoId, estado)).build());
    }

    /* ─── evaluar matrícula (dispara emisión de certificado) ─── */

    /**
     * Variante admin de {@code POST /api/matriculas/{id}/evaluar}. Permite al
     * staff revisar el progreso de un alumno y forzar la evaluación que
     * eventualmente emite el certificado. Idempotente — el MS de certificados
     * no genera duplicados.
     */
    @PostMapping("/matriculas/{id}/evaluar")
    public ResponseEntity<GeneralResponseDTO<AprobacionResultDTO>> evaluar(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        ControllerSecurity.requireAdmin(rolesHeader);
        AprobacionResultDTO data = aprobacionService.evaluar(id);
        return ResponseEntity.ok(GeneralResponseDTO.<AprobacionResultDTO>builder()
                .statusCode(200).serviceName(SERVICE)
                .message(data.isAprobado()
                        ? "Matrícula evaluada · aprobada"
                        : "Aún no cumple los criterios")
                .response(data).build());
    }

    /* ─── job manual: forzar liberación de reservas vencidas ─── */

    @PostMapping("/matriculas/liberar-reservas-vencidas")
    public ResponseEntity<GeneralResponseDTO<Integer>> liberarReservasVencidas(
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {

        ControllerSecurity.requireAdmin(rolesHeader);
        int liberadas = matriculaService.liberarReservasVencidas();
        return ResponseEntity.ok(GeneralResponseDTO.<Integer>builder()
                .statusCode(200).serviceName(SERVICE)
                .message(liberadas + " reservas liberadas")
                .response(liberadas).build());
    }
}
