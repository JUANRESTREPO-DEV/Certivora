package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.ActualizarProgresoRequest;
import com.eduessence.cursos.model.dto.response.MatriculaResponse;
import com.eduessence.cursos.model.entity.ProgresoLeccion;
import com.eduessence.cursos.repository.ProgresoLeccionRepository;
import com.eduessence.cursos.service.ProgresoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Progreso")
@RestController
@RequestMapping("/api/progreso")
@RequiredArgsConstructor
public class ProgresoController {

    private static final String SERVICE = "cursos-service";
    private final ProgresoService progresoService;
    private final ProgresoLeccionRepository progresoRepository;

    @PutMapping("/matricula/{matriculaId}/leccion/{leccionId}")
    public ResponseEntity<GeneralResponseDTO<MatriculaResponse>> actualizar(
            @PathVariable Long matriculaId,
            @PathVariable Long leccionId,
            @Valid @RequestBody ActualizarProgresoRequest req
    ) {
        return ResponseEntity.ok(GeneralResponseDTO.<MatriculaResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("Progreso actualizado")
                .response(progresoService.actualizar(matriculaId, leccionId, req)).build());
    }

    /**
     * Devuelve el progreso por lección del alumno como un mapa
     * {@code leccionId → estado} (NO_INICIADA / EN_PROGRESO / COMPLETADA).
     * Sirve para que el front marque visualmente qué lecciones ya completó.
     */
    @GetMapping("/matricula/{matriculaId}")
    public ResponseEntity<GeneralResponseDTO<Map<String, String>>> porMatricula(
            @PathVariable Long matriculaId) {
        Map<String, String> out = new LinkedHashMap<>();
        for (ProgresoLeccion p : progresoRepository.findAllByMatriculaId(matriculaId)) {
            out.put(String.valueOf(p.getLeccionId()), p.getEstado().name());
        }
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, String>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(out).build());
    }
}
