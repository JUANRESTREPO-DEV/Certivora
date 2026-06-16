package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.ActualizarConfigAprobacionRequest;
import com.eduessence.cursos.model.dto.response.ConfigAprobacionResponse;
import com.eduessence.cursos.service.ConfigAprobacionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Configuración de aprobación")
@RestController
@RequestMapping("/api/cursos/{cursoId}/configuracion-aprobacion")
@RequiredArgsConstructor
public class ConfigAprobacionController {

    private static final String SERVICE = "cursos-service";
    private final ConfigAprobacionService service;

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<ConfigAprobacionResponse>> obtener(@PathVariable Long cursoId) {
        return ResponseEntity.ok(ok(service.obtener(cursoId), "OK"));
    }

    @PutMapping
    public ResponseEntity<GeneralResponseDTO<ConfigAprobacionResponse>> actualizar(
            @PathVariable Long cursoId,
            @Valid @RequestBody ActualizarConfigAprobacionRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(cursoId, req), "Configuración actualizada"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
