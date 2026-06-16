package com.eduessence.sendmail.controller;

import com.eduessence.sendmail.model.dto.GeneralResponseDTO;
import com.eduessence.sendmail.model.dto.PreviewRequest;
import com.eduessence.sendmail.model.dto.PreviewResponse;
import com.eduessence.sendmail.model.dto.TemplateRequest;
import com.eduessence.sendmail.model.dto.TemplateResponse;
import com.eduessence.sendmail.model.dto.TestSendRequest;
import com.eduessence.sendmail.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Templates", description = "Administración de plantillas de email")
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private static final String SERVICE = "sendmail-service";

    private final TemplateService templateService;

    @Operation(summary = "Listar templates",
            description = "Por defecto solo activos. Pasar incluirInactivos=true para ver todos (admin).")
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<List<TemplateResponse>>> listar(
            @RequestParam(value = "incluirInactivos", required = false, defaultValue = "false")
            boolean incluirInactivos
    ) {
        List<TemplateResponse> data = incluirInactivos
                ? templateService.listarTodos()
                : templateService.listarActivos();
        return ok(data, "OK");
    }

    @Operation(summary = "Obtener template por id")
    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> obtener(@PathVariable Long id) {
        return ok(templateService.obtener(id), "OK");
    }

    @Operation(summary = "Crear template")
    @PostMapping
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> crear(
            @Valid @RequestBody TemplateRequest request
    ) {
        TemplateResponse data = templateService.crear(request);
        return ResponseEntity.status(201)
                .body(GeneralResponseDTO.<TemplateResponse>builder()
                        .statusCode(201).serviceName(SERVICE).message("Template creado").response(data).build());
    }

    @Operation(summary = "Actualizar template")
    @PutMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TemplateRequest request
    ) {
        return ok(templateService.actualizar(id, request), "Template actualizado");
    }

    @Operation(summary = "Activar template")
    @PutMapping("/{id}/activar")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> activar(@PathVariable Long id) {
        return ok(templateService.cambiarEstado(id, true), "Template activado");
    }

    @Operation(summary = "Desactivar template")
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<GeneralResponseDTO<TemplateResponse>> desactivar(@PathVariable Long id) {
        return ok(templateService.cambiarEstado(id, false), "Template desactivado");
    }

    /* ───────────────── HTML del template (S3) ───────────────── */

    @Operation(summary = "Descargar HTML crudo del template desde S3")
    @GetMapping(value = "/{id}/html", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneralResponseDTO<Map<String, String>>> obtenerHtml(@PathVariable Long id) {
        String html = templateService.obtenerHtml(id);
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, String>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(Map.of("html", html == null ? "" : html))
                .build());
    }

    @Operation(summary = "Guardar HTML del template en S3 (sobrescribe el anterior)")
    @PutMapping(value = "/{id}/html", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneralResponseDTO<Map<String, String>>> guardarHtml(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        templateService.guardarHtml(id, body == null ? null : body.get("html"));
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, String>>builder()
                .statusCode(200).serviceName(SERVICE).message("HTML guardado en S3")
                .response(Map.of("status", "ok"))
                .build());
    }

    /* ───────────────── Preview & Test send ───────────────── */

    @Operation(summary = "Previsualizar el render del template con variables de prueba",
            description = "No envía nada — solo devuelve el HTML renderizado para mostrar en la UI.")
    @PostMapping(value = "/{id}/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneralResponseDTO<PreviewResponse>> preview(
            @PathVariable Long id,
            @RequestBody(required = false) PreviewRequest request
    ) {
        PreviewResponse data = templateService.previsualizar(id, request);
        return ok(data, "OK");
    }

    @Operation(summary = "Enviar correo de prueba con el template y variables provistas")
    @PostMapping(value = "/{id}/test-send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneralResponseDTO<Map<String, String>>> testSend(
            @PathVariable Long id,
            @Valid @RequestBody TestSendRequest request
    ) {
        String msg = templateService.enviarPrueba(id, request);
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, String>>builder()
                .statusCode(200).serviceName(SERVICE).message(msg)
                .response(Map.of("status", "ok"))
                .build());
    }

    private <T> ResponseEntity<GeneralResponseDTO<T>> ok(T data, String msg) {
        return ResponseEntity.ok(GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build());
    }
}
