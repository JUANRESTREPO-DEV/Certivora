package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.ActualizarCursoRequest;
import com.eduessence.cursos.model.dto.request.CrearCursoRequest;
import com.eduessence.cursos.model.dto.response.CursoResponse;
import com.eduessence.cursos.model.enums.EstadoCurso;
import com.eduessence.cursos.service.CursoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cursos")
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    private static final String SERVICE = "cursos-service";
    private final CursoService cursoService;

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<CursoResponse>> crear(@Valid @RequestBody CrearCursoRequest req) {
        return ResponseEntity.status(201).body(ok(cursoService.crear(req), "Curso creado"));
    }

    /**
     * Listado admin — incluye BORRADOR + ACTIVO + ARCHIVADO. Acepta filtro
     * opcional por estado. Requiere permiso CURSO_LISTAR (admin/gerente).
     *
     * GET /api/cursos               → todos los estados ordenados por creación
     * GET /api/cursos?estado=BORRADOR  → solo borradores pendientes de activar
     */
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<CursoResponse>>> listarAdmin(
            @RequestParam(required = false) EstadoCurso estado) {
        return ResponseEntity.ok(ok(cursoService.listarTodos(estado), "OK"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<CursoResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarCursoRequest req) {
        return ResponseEntity.ok(ok(cursoService.actualizar(id, req), "Curso actualizado"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<CursoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(cursoService.obtener(id), "OK"));
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<GeneralResponseDTO<CursoResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ok(cursoService.activar(id), "Curso activado"));
    }

    @PutMapping("/{id}/archivar")
    public ResponseEntity<GeneralResponseDTO<CursoResponse>> archivar(@PathVariable Long id) {
        return ResponseEntity.ok(ok(cursoService.archivar(id), "Curso archivado"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
