package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CrearSeccionRequest;
import com.eduessence.cursos.model.dto.response.SeccionResponse;
import com.eduessence.cursos.service.SeccionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Secciones del curso")
@RestController
@RequestMapping("/api/cursos/{cursoId}/secciones")
@RequiredArgsConstructor
public class SeccionController {

    private static final String SERVICE = "cursos-service";
    private final SeccionService service;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<SeccionResponse>>> listar(@PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(service.listar(cursoId), "OK"));
    }

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<SeccionResponse>> crear(
            @PathVariable Long cursoId, @Valid @RequestBody CrearSeccionRequest req) {
        return ResponseEntity.status(201).body(ok(service.crear(cursoId, req), "Sección creada"));
    }

    @PutMapping("/{seccionId}")
    public ResponseEntity<GeneralResponseDTO<SeccionResponse>> actualizar(
            @PathVariable Long cursoId, @PathVariable Long seccionId,
            @Valid @RequestBody CrearSeccionRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(cursoId, seccionId, req), "Sección actualizada"));
    }

    @DeleteMapping("/{seccionId}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(
            @PathVariable Long cursoId, @PathVariable Long seccionId) {
        service.borrar(cursoId, seccionId);
        return ResponseEntity.ok(ok(null, "Sección eliminada"));
    }

    @PutMapping("/orden")
    public ResponseEntity<GeneralResponseDTO<Void>> reordenar(
            @PathVariable Long cursoId, @RequestBody List<Long> idsEnOrden) {
        service.reordenar(cursoId, idsEnOrden);
        return ResponseEntity.ok(ok(null, "Orden actualizado"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
