package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CrearExamenRequest;
import com.eduessence.cursos.model.dto.response.ExamenResponse;
import com.eduessence.cursos.service.ExamenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Exámenes / Quizzes")
@RestController
@RequestMapping("/api/cursos/{cursoId}/examenes")
@RequiredArgsConstructor
public class ExamenController {

    private static final String SERVICE = "cursos-service";
    private final ExamenService service;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<ExamenResponse>>> listar(@PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(service.listarPorCurso(cursoId), "OK"));
    }

    @GetMapping("/{examenId}")
    public ResponseEntity<GeneralResponseDTO<ExamenResponse>> obtener(
            @PathVariable Long cursoId, @PathVariable Long examenId) {
        return ResponseEntity.ok(ok(service.obtener(cursoId, examenId), "OK"));
    }

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<ExamenResponse>> crear(
            @PathVariable Long cursoId, @Valid @RequestBody CrearExamenRequest req) {
        return ResponseEntity.status(201).body(ok(service.crear(cursoId, req), "Examen creado"));
    }

    @PutMapping("/{examenId}")
    public ResponseEntity<GeneralResponseDTO<ExamenResponse>> actualizar(
            @PathVariable Long cursoId, @PathVariable Long examenId,
            @Valid @RequestBody CrearExamenRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(cursoId, examenId, req), "Examen actualizado"));
    }

    @DeleteMapping("/{examenId}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(
            @PathVariable Long cursoId, @PathVariable Long examenId) {
        service.borrar(cursoId, examenId);
        return ResponseEntity.ok(ok(null, "Examen eliminado"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
