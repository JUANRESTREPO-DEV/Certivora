package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.GuardarRespuestaRequest;
import com.eduessence.cursos.model.dto.response.IntentoExamenResponse;
import com.eduessence.cursos.service.IntentoExamenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del flujo de toma de examen por parte del alumno.
 *
 * <pre>
 *   POST   /api/matriculas/{mid}/examenes/{eid}/intento     iniciar()
 *   GET    /api/matriculas/{mid}/examenes/{eid}/intentos    listar()
 *   PUT    /api/intentos/{intentoId}/respuesta              guardarRespuesta()
 *   POST   /api/intentos/{intentoId}/finalizar              finalizar()
 *   GET    /api/intentos/{intentoId}                        obtener()
 * </pre>
 */
@Tag(name = "Intentos de examen")
@RestController
@RequiredArgsConstructor
public class IntentoExamenController {

    private static final String SERVICE = "cursos-service";
    private final IntentoExamenService service;

    @PostMapping("/api/matriculas/{matriculaId}/examenes/{examenId}/intento")
    public ResponseEntity<GeneralResponseDTO<IntentoExamenResponse>> iniciar(
            @PathVariable Long matriculaId,
            @PathVariable Long examenId) {
        return ResponseEntity.status(201).body(ok(
                service.iniciar(matriculaId, examenId),
                "Intento iniciado"));
    }

    @GetMapping("/api/matriculas/{matriculaId}/examenes/{examenId}/intentos")
    public ResponseEntity<GeneralResponseDTO<List<IntentoExamenResponse>>> listar(
            @PathVariable Long matriculaId,
            @PathVariable Long examenId) {
        return ResponseEntity.ok(ok(
                service.listarPorMatriculaYExamen(matriculaId, examenId), "OK"));
    }

    @PutMapping("/api/intentos/{intentoId}/respuesta")
    public ResponseEntity<GeneralResponseDTO<IntentoExamenResponse>> guardarRespuesta(
            @PathVariable Long intentoId,
            @Valid @RequestBody GuardarRespuestaRequest req) {
        return ResponseEntity.ok(ok(
                service.guardarRespuesta(intentoId, req), "Respuesta guardada"));
    }

    @PostMapping("/api/intentos/{intentoId}/finalizar")
    public ResponseEntity<GeneralResponseDTO<IntentoExamenResponse>> finalizar(
            @PathVariable Long intentoId) {
        return ResponseEntity.ok(ok(
                service.finalizar(intentoId), "Intento finalizado"));
    }

    @GetMapping("/api/intentos/{intentoId}")
    public ResponseEntity<GeneralResponseDTO<IntentoExamenResponse>> obtener(
            @PathVariable Long intentoId) {
        return ResponseEntity.ok(ok(service.obtener(intentoId), "OK"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
