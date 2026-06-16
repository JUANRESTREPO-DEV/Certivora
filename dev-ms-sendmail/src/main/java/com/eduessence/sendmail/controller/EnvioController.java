package com.eduessence.sendmail.controller;

import com.eduessence.sendmail.model.dto.EmailEnvioResponse;
import com.eduessence.sendmail.model.dto.GeneralResponseDTO;
import com.eduessence.sendmail.model.dto.PageResponse;
import com.eduessence.sendmail.model.enums.EstadoEnvio;
import com.eduessence.sendmail.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Bandeja admin del histórico de envíos. Vive bajo {@code /sendmail/api}
 * (requiere JWT vía gateway).
 */
@Tag(name = "Envíos", description = "Histórico de envíos y reintentos")
@RestController
@RequestMapping("/api/envios")
@RequiredArgsConstructor
public class EnvioController {

    private static final String SERVICE = "sendmail-service";

    private final EnvioService envioService;

    @Operation(summary = "Buscar envíos paginados con filtros opcionales")
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<PageResponse<EmailEnvioResponse>>> buscar(
            @RequestParam(required = false) EstadoEnvio estado,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        PageResponse<EmailEnvioResponse> data =
                envioService.buscar(estado, templateId, desde, hasta, q, page, size);
        return ok(data, "OK");
    }

    @Operation(summary = "Detalle de un envío (variables + error si aplica)")
    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<EmailEnvioResponse>> obtener(@PathVariable Long id) {
        return ok(envioService.obtener(id), "OK");
    }

    @Operation(summary = "Reintenta un envío fallido o ya exitoso (idempotente)")
    @PostMapping("/{id}/reintentar")
    public ResponseEntity<GeneralResponseDTO<EmailEnvioResponse>> reintentar(@PathVariable Long id) {
        EmailEnvioResponse data = envioService.reintentar(id);
        return ok(data, "Reintento procesado");
    }

    @Operation(summary = "Resumen { ENVIADO, FALLIDO, REINTENTAR, TOTAL }")
    @GetMapping("/resumen")
    public ResponseEntity<GeneralResponseDTO<Map<String, Long>>> resumen() {
        return ok(envioService.resumen(), "OK");
    }

    private <T> ResponseEntity<GeneralResponseDTO<T>> ok(T data, String msg) {
        return ResponseEntity.ok(GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build());
    }
}
