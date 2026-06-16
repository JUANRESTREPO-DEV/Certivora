package com.eduessence.cursos.controller;

import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import com.eduessence.cursos.model.dto.request.CrearRecursoRequest;
import com.eduessence.cursos.model.dto.request.PresignedUrlRequest;
import com.eduessence.cursos.model.dto.response.PresignedUrlResponse;
import com.eduessence.cursos.model.dto.response.RecursoResponse;
import com.eduessence.cursos.model.enums.TipoRecurso;
import com.eduessence.cursos.service.CursoRecursoService;
import com.eduessence.cursos.service.S3PresignedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Recursos del curso")
@RestController
@RequestMapping("/api/cursos/{cursoId}/recursos")
@RequiredArgsConstructor
public class CursoRecursoController {

    private static final String SERVICE = "cursos-service";

    private final CursoRecursoService service;
    private final S3PresignedService s3;

    /** Lista todos los recursos del curso (opcionalmente filtra por tipo). */
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<RecursoResponse>>> listar(
            @PathVariable Long cursoId,
            @RequestParam(required = false) TipoRecurso tipo) {
        var data = (tipo == null) ? service.listar(cursoId) : service.listarPorTipo(cursoId, tipo);
        return ResponseEntity.ok(ok(data, "OK"));
    }

    /** Persiste un recurso ya subido a S3. */
    @PostMapping
    public ResponseEntity<GeneralResponseDTO<RecursoResponse>> crear(
            @PathVariable Long cursoId,
            @Valid @RequestBody CrearRecursoRequest req) {
        return ResponseEntity.status(201).body(ok(service.crear(cursoId, req), "Recurso creado"));
    }

    @PutMapping("/{recursoId}")
    public ResponseEntity<GeneralResponseDTO<RecursoResponse>> actualizar(
            @PathVariable Long cursoId,
            @PathVariable Long recursoId,
            @Valid @RequestBody CrearRecursoRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(cursoId, recursoId, req), "Recurso actualizado"));
    }

    @DeleteMapping("/{recursoId}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(
            @PathVariable Long cursoId,
            @PathVariable Long recursoId) {
        service.borrar(cursoId, recursoId);
        return ResponseEntity.ok(ok(null, "Recurso eliminado"));
    }

    /** Reorden por lista de IDs. El índice en la lista = orden final. */
    @PutMapping("/orden")
    public ResponseEntity<GeneralResponseDTO<Void>> reordenar(
            @PathVariable Long cursoId,
            @RequestBody List<Long> recursoIdsEnOrden) {
        service.reordenar(cursoId, recursoIdsEnOrden);
        return ResponseEntity.ok(ok(null, "Orden actualizado"));
    }

    /**
     * Solicita una URL pre-firmada de S3 para subir un archivo. El front sube
     * los bytes directamente al bucket y luego invoca POST {@code /recursos}
     * con la {@code s3Key} devuelta para persistir la metadata.
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<GeneralResponseDTO<PresignedUrlResponse>> presignedUrl(
            @PathVariable Long cursoId,
            @Valid @RequestBody PresignedUrlRequest req) {
        return ResponseEntity.ok(ok(s3.generarUploadUrl(cursoId, req), "URL pre-firmada generada"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
