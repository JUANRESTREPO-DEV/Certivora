package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CheckinScanRequest;
import com.eduessence.cursos.model.dto.response.AsistenteCheckinResponse;
import com.eduessence.cursos.model.dto.response.CheckinItemResponse;
import com.eduessence.cursos.model.dto.response.CheckinScanResponse;
import com.eduessence.cursos.model.dto.response.CursoCheckinResponse;
import com.eduessence.cursos.model.dto.response.SesionCheckinResponse;
import com.eduessence.cursos.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel de check-in presencial. Lo usa el operador (staff del evento) desde
 * su tablet/celular. Escanea el QR de la escarapela del asistente y
 * registra su asistencia a la sesión seleccionada.
 */
@Tag(name = "Check-in presencial")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private static final String SERVICE = "cursos-service";
    private final CheckinService checkinService;

    @Operation(summary = "Cursos presenciales — vista inicial del panel de check-in")
    @GetMapping("/mis-cursos-presenciales")
    public ResponseEntity<GeneralResponseDTO<List<CursoCheckinResponse>>> misCursosPresenciales(
            @RequestParam(defaultValue = "false") boolean todos,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        // Si todos=true (operador), no filtra por usuario; si false, filtra al user del JWT
        Long userId = null;
        if (!todos && userIdHeader != null && !userIdHeader.isBlank()) {
            try { userId = Long.valueOf(userIdHeader); } catch (NumberFormatException ignored) {}
        }
        return ResponseEntity.ok(ok(checkinService.misCursosPresenciales(userId), "OK"));
    }

    @Operation(summary = "Check-ins existentes de un curso (todas las sesiones)")
    @GetMapping("/cursos/{cursoId}/checkins")
    public ResponseEntity<GeneralResponseDTO<List<CheckinItemResponse>>> checkinsCurso(
            @PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(checkinService.checkinsDelCurso(cursoId), "OK"));
    }

    @Operation(summary = "Sesiones presenciales de un curso con estado check-in")
    @GetMapping("/cursos/{cursoId}/sesiones")
    public ResponseEntity<GeneralResponseDTO<List<SesionCheckinResponse>>> sesionesDelCurso(
            @PathVariable Long cursoId,
            @RequestParam(defaultValue = "false") boolean soloMisSesiones,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long soloParaUser = null;
        if (soloMisSesiones && userIdHeader != null && !userIdHeader.isBlank()) {
            try { soloParaUser = Long.valueOf(userIdHeader); } catch (NumberFormatException ignored) {}
        }
        return ResponseEntity.ok(ok(checkinService.sesionesDelCurso(cursoId, soloParaUser), "OK"));
    }

    @Operation(summary = "Sesiones presenciales del día")
    @GetMapping("/sesiones")
    public ResponseEntity<GeneralResponseDTO<List<SesionCheckinResponse>>> sesiones(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "false") boolean soloMisSesiones,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long soloParaUser = null;
        if (soloMisSesiones && userIdHeader != null && !userIdHeader.isBlank()) {
            try { soloParaUser = Long.valueOf(userIdHeader); } catch (NumberFormatException ignored) {}
        }
        return ResponseEntity.ok(ok(checkinService.sesionesDeDia(fecha, soloParaUser), "OK"));
    }

    @Operation(summary = "Asistentes matriculados de una sesión + estado check-in (admin)")
    @GetMapping("/sesiones/{sesionId}/asistentes")
    public ResponseEntity<GeneralResponseDTO<List<AsistenteCheckinResponse>>> asistentes(
            @PathVariable Long sesionId) {
        return ResponseEntity.ok(ok(checkinService.asistentesDeSesion(sesionId), "OK"));
    }

    @Operation(summary = "Mi propia asistencia en una sesión (asistente)")
    @GetMapping("/sesiones/{sesionId}/mi-asistencia")
    public ResponseEntity<GeneralResponseDTO<AsistenteCheckinResponse>> miAsistencia(
            @PathVariable Long sesionId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.ok(ok(null, "Sin sesión de usuario"));
        }
        try {
            Long userId = Long.valueOf(userIdHeader);
            return ResponseEntity.ok(ok(
                    checkinService.miAsistenciaEnSesion(sesionId, userId), "OK"));
        } catch (NumberFormatException ignored) {
            return ResponseEntity.ok(ok(null, "Header X-User-Id inválido"));
        }
    }

    @Operation(summary = "Registrar check-in por escaneo QR o manual")
    @PostMapping("/scan")
    public ResponseEntity<GeneralResponseDTO<CheckinScanResponse>> scan(
            @Valid @RequestBody CheckinScanRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long operador = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try { operador = Long.valueOf(userIdHeader); } catch (NumberFormatException ignored) {}
        }
        CheckinScanResponse res = checkinService.scan(req, operador);
        String msg = "OK".equals(res.getEstado()) ? "Check-in registrado" : res.getMensaje();
        return ResponseEntity.ok(ok(res, msg));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
