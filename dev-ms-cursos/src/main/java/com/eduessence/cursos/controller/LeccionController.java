package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CrearLeccionRequest;
import com.eduessence.cursos.model.dto.response.LeccionResponse;
import com.eduessence.cursos.service.LeccionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lecciones")
@RestController
@RequestMapping("/api/cursos/{cursoId}/secciones/{seccionId}/lecciones")
@RequiredArgsConstructor
public class LeccionController {

    private static final String SERVICE = "cursos-service";
    private final LeccionService service;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<LeccionResponse>>> listar(
            @PathVariable Long cursoId, @PathVariable Long seccionId) {
        return ResponseEntity.ok(ok(service.listar(seccionId), "OK"));
    }

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<LeccionResponse>> crear(
            @PathVariable Long cursoId, @PathVariable Long seccionId,
            @Valid @RequestBody CrearLeccionRequest req) {
        return ResponseEntity.status(201).body(ok(service.crear(seccionId, req), "Lección creada"));
    }

    @PutMapping("/{leccionId}")
    public ResponseEntity<GeneralResponseDTO<LeccionResponse>> actualizar(
            @PathVariable Long cursoId, @PathVariable Long seccionId,
            @PathVariable Long leccionId, @Valid @RequestBody CrearLeccionRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(seccionId, leccionId, req), "Lección actualizada"));
    }

    @DeleteMapping("/{leccionId}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(
            @PathVariable Long cursoId, @PathVariable Long seccionId, @PathVariable Long leccionId) {
        service.borrar(seccionId, leccionId);
        return ResponseEntity.ok(ok(null, "Lección eliminada"));
    }

    @PutMapping("/orden")
    public ResponseEntity<GeneralResponseDTO<Void>> reordenar(
            @PathVariable Long cursoId, @PathVariable Long seccionId,
            @RequestBody List<Long> idsEnOrden) {
        service.reordenar(seccionId, idsEnOrden);
        return ResponseEntity.ok(ok(null, "Orden actualizado"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
