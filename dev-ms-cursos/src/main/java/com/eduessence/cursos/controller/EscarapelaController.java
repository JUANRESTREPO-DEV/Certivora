package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.EscarapelaTemplateRequest;
import com.eduessence.cursos.model.dto.response.EscarapelaRenderResponse;
import com.eduessence.cursos.model.dto.response.EscarapelaTemplateResponse;
import com.eduessence.cursos.service.EscarapelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints admin de la plantilla de escarapela + endpoint público para el
 * render por token. La ruta pública queda bajo {@code /api/public} para que
 * el gateway la deje pasar sin JWT (misma convención que los archivos).
 */
@Tag(name = "Escarapelas")
@RestController
@RequiredArgsConstructor
public class EscarapelaController {

    private static final String SERVICE = "cursos-service";
    private final EscarapelaService escarapelaService;

    /* ─────────── Admin ─────────── */

    @Operation(summary = "Obtener plantilla de escarapela de un curso")
    @GetMapping("/api/cursos/{cursoId}/escarapela-template")
    public ResponseEntity<GeneralResponseDTO<EscarapelaTemplateResponse>> obtener(@PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(escarapelaService.obtenerPorCurso(cursoId),
                "OK"));
    }

    @Operation(summary = "Crear o actualizar la plantilla de escarapela de un curso")
    @PutMapping("/api/cursos/{cursoId}/escarapela-template")
    public ResponseEntity<GeneralResponseDTO<EscarapelaTemplateResponse>> upsert(
            @PathVariable Long cursoId,
            @Valid @RequestBody EscarapelaTemplateRequest req) {
        return ResponseEntity.ok(ok(escarapelaService.upsert(cursoId, req),
                "Escarapela guardada"));
    }

    /* ─────────── Público (por token de matrícula) ─────────── */

    @Operation(summary = "Render público de la escarapela por token")
    @GetMapping("/api/public/escarapela/{token}")
    public ResponseEntity<GeneralResponseDTO<EscarapelaRenderResponse>> render(@PathVariable String token) {
        return ResponseEntity.ok(ok(escarapelaService.renderPorToken(token), "OK"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
