package com.eduessence.certificados.controller;

import com.eduessence.certificados.model.dto.GeneralResponseDTO;
import com.eduessence.certificados.model.dto.request.CrearTemplateRequest;
import com.eduessence.certificados.model.dto.response.TemplateResponse;
import com.eduessence.certificados.service.DiplomaTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Plantillas de diploma")
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class DiplomaTemplateController {

    private static final String SERVICE = "certificados-service";
    private final DiplomaTemplateService service;

    /** Lista plantillas activas. Opcionalmente filtra por nombre (q) o por curso. */
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<TemplateResponse>>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cursoId) {
        List<TemplateResponse> data = (cursoId != null)
                ? service.listarParaCurso(cursoId)
                : service.listar(q);
        return ResponseEntity.ok(ok(data, "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(service.obtener(id), "OK"));
    }

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> crear(
            @Valid @RequestBody CrearTemplateRequest req) {
        return ResponseEntity.status(201).body(ok(service.crear(req), "Plantilla creada"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CrearTemplateRequest req) {
        return ResponseEntity.ok(ok(service.actualizar(id, req), "Plantilla actualizada"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(@PathVariable Long id) {
        service.borrar(id);
        return ResponseEntity.ok(ok(null, "Plantilla desactivada"));
    }

    /**
     * Clona una plantilla existente vinculándola a un curso específico. Lo
     * llama el form admin de cursos cuando el usuario elige "plantilla
     * existente" — en vez de compartir el mismo registro entre cursos, cada
     * curso obtiene su propia copia con su {@code curso_id} y
     * {@code tipo_participante}.
     *
     * Body: {@code {"cursoId": Long, "tipoParticipante"?: String, "nombre"?: String}}
     */
    @PostMapping("/{id}/clonar")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> clonar(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
        Object cursoIdObj = body == null ? null : body.get("cursoId");
        if (!(cursoIdObj instanceof Number n)) {
            return ResponseEntity.badRequest().body(GeneralResponseDTO.<TemplateResponse>builder()
                    .statusCode(400).serviceName(SERVICE).message("cursoId es obligatorio")
                    .response(null).build());
        }
        String tipo = body.get("tipoParticipante") == null ? null : String.valueOf(body.get("tipoParticipante"));
        String nombre = body.get("nombre") == null ? null : String.valueOf(body.get("nombre"));
        TemplateResponse copia = service.clonar(id, n.longValue(), tipo, nombre);
        return ResponseEntity.status(201).body(ok(copia, "Plantilla clonada"));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }
}
