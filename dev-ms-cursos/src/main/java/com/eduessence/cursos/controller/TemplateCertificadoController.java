package com.eduessence.cursos.controller;

import com.eduessence.cursos.feign.CertificadosServiceClient;
import com.eduessence.cursos.model.dto.GeneralResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Proxy front-friendly hacia el catálogo de plantillas de certificado.
 * Evita que el front tenga que conocer el ruteo a certificados-service y
 * unifica el manejo de wrapper {@code GeneralResponseDTO}.
 */
@Tag(name = "Plantillas de certificado")
@RestController
@RequestMapping("/api/cursos/templates-certificado")
@RequiredArgsConstructor
public class TemplateCertificadoController {

    private static final String SERVICE = "cursos-service";
    private final CertificadosServiceClient client;

    @GetMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<GeneralResponseDTO<List<Map<String, Object>>>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cursoId) {
        Map<String, Object> raw = client.listarTemplates(q, cursoId);
        Object resp = raw == null ? null : raw.get("response");
        List<Map<String, Object>> data = (resp instanceof List<?> l) ? (List<Map<String, Object>>) (List<?>) l : List.of();
        return ResponseEntity.ok(GeneralResponseDTO.<List<Map<String, Object>>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    /**
     * Crea una plantilla nueva. Body acepta los mismos campos que
     * certificados-service.CrearTemplateRequest (nombre, descripcion, cursoId,
     * tipoParticipante, templateUrl, posiciones). Devuelve la plantilla creada
     * con su {@code id}, que el front guarda en {@code curso.templateCertificadoId}.
     */
    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> crear(
            @RequestBody Map<String, Object> body) {
        Map<String, Object> raw = client.crearTemplate(body);
        Object resp = raw == null ? null : raw.get("response");
        Map<String, Object> data = (resp instanceof Map<?, ?> m) ? (Map<String, Object>) m : Map.of();
        return ResponseEntity.status(201).body(GeneralResponseDTO.<Map<String, Object>>builder()
                .statusCode(201).serviceName(SERVICE).message("Plantilla creada").response(data).build());
    }

    /**
     * Clona una plantilla existente vinculándola al curso indicado en el
     * body. Lo usa el form admin de cursos cuando el usuario elige
     * "Plantilla existente": se crea una copia con {@code curso_id} del
     * curso actual para evitar compartir el registro con otros cursos.
     *
     * Body: {@code {"cursoId": Long, "tipoParticipante"?: String, "nombre"?: String}}
     */
    @PostMapping("/{id}/clonar")
    @SuppressWarnings("unchecked")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> clonar(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> raw = client.clonarTemplate(id, body);
        Object resp = raw == null ? null : raw.get("response");
        Map<String, Object> data = (resp instanceof Map<?, ?> m) ? (Map<String, Object>) m : Map.of();
        return ResponseEntity.status(201).body(GeneralResponseDTO.<Map<String, Object>>builder()
                .statusCode(201).serviceName(SERVICE).message("Plantilla clonada").response(data).build());
    }

    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> obtener(@PathVariable Long id) {
        Map<String, Object> raw = client.obtenerTemplate(id);
        Object resp = raw == null ? null : raw.get("response");
        Map<String, Object> data = (resp instanceof Map<?, ?> m) ? (Map<String, Object>) m : Map.of();
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, Object>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> actualizar(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> raw = client.actualizarTemplate(id, body);
        Object resp = raw == null ? null : raw.get("response");
        Map<String, Object> data = (resp instanceof Map<?, ?> m) ? (Map<String, Object>) m : Map.of();
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, Object>>builder()
                .statusCode(200).serviceName(SERVICE).message("Plantilla actualizada").response(data).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<Void>> borrar(@PathVariable Long id) {
        client.borrarTemplate(id);
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200).serviceName(SERVICE).message("Plantilla desactivada").response(null).build());
    }
}
