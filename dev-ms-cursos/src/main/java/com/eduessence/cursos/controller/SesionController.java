package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CrearSesionRequest;
import com.eduessence.cursos.model.dto.response.SesionResponse;
import com.eduessence.cursos.service.SesionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sesiones del curso")
@RestController
@RequestMapping("/api/cursos/{cursoId}/sesiones")
@RequiredArgsConstructor
public class SesionController {

    private static final String SERVICE = "cursos-service";
    private final SesionService service;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<SesionResponse>>> listar(@PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(service.listar(cursoId), "OK"));
    }

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<SesionResponse>> crear(
            @PathVariable Long cursoId, @Valid @RequestBody CrearSesionRequest req) {
        return ResponseEntity.status(201).body(ok(service.crear(cursoId, req), "Sesión creada"));
    }

    @PutMapping("/{sesionId}")
    public ResponseEntity<GeneralResponseDTO<SesionResponse>> actualizar(
            @PathVariable Long cursoId, @PathVariable Long sesionId,
            @Valid @RequestBody CrearSesionRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(cursoId, sesionId, req), "Sesión actualizada"));
    }

    @DeleteMapping("/{sesionId}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(
            @PathVariable Long cursoId, @PathVariable Long sesionId) {
        service.borrar(cursoId, sesionId);
        return ResponseEntity.ok(ok(null, "Sesión eliminada"));
    }

    /** Regenera credenciales OBS / playback para una sesión VIRTUAL existente. */
    @PostMapping("/{sesionId}/regenerar-stream")
    public ResponseEntity<GeneralResponseDTO<SesionResponse>> regenerarStream(
            @PathVariable Long cursoId, @PathVariable Long sesionId) {
        return ResponseEntity.ok(ok(
                service.regenerarCredencialesStream(cursoId, sesionId),
                "Credenciales regeneradas"));
    }

    /** Termina la transmisión en vivo y libera el canal del provider (AWS IVS, etc.). */
    @PostMapping("/{sesionId}/terminar")
    public ResponseEntity<GeneralResponseDTO<SesionResponse>> terminar(
            @PathVariable Long cursoId, @PathVariable Long sesionId) {
        return ResponseEntity.ok(ok(
                service.terminarTransmision(cursoId, sesionId),
                "Transmisión terminada"));
    }

    /** Cancela una sesión programada y libera recursos asociados. */
    @PostMapping("/{sesionId}/cancelar")
    public ResponseEntity<GeneralResponseDTO<SesionResponse>> cancelar(
            @PathVariable Long cursoId, @PathVariable Long sesionId,
            @RequestParam(value = "razon", required = false) String razon) {
        return ResponseEntity.ok(ok(
                service.cancelar(cursoId, sesionId, razon),
                "Sesión cancelada"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
