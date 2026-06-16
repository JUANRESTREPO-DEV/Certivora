package com.eduessence.pagos.controller;

import com.eduessence.pagos.exception.PagosApiException;
import com.eduessence.pagos.exception.ServerApiStatusCode;
import com.eduessence.pagos.model.dto.GeneralResponseDTO;
import com.eduessence.pagos.model.dto.response.PagoResponse;
import com.eduessence.pagos.service.ComprobanteStorageService;
import com.eduessence.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * Endpoints para el comprobante de transferencia bancaria:
 *
 * <pre>
 *   POST /pagos/api/pagos/{id}/comprobante
 *      Usuario sube su comprobante (multipart). Estado pasa a
 *      PENDIENTE_CONFIRMACION. Admin debe aprobarlo manualmente.
 *
 *   GET  /pagos/api/public/comprobantes/stream?key=...
 *      Sirve el binario desde S3 a través del backend (evita CORS y oculta
 *      el bucket). La URL la genera ComprobanteStorageService al subir.
 * </pre>
 */
@Tag(name = "Comprobantes")
@RestController
@RequiredArgsConstructor
public class ComprobanteController {

    private static final String SERVICE = "pagos-service";

    private final PagoService pagoService;
    private final ComprobanteStorageService storage;

    @PostMapping(value = "/api/pagos/{id}/comprobante",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneralResponseDTO<PagoResponse>> subir(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "observacion", required = false) String observacion,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = parseUserId(userIdHeader);
        PagoResponse data = pagoService.subirComprobante(id, userId, file, observacion);
        return ResponseEntity.ok(GeneralResponseDTO.<PagoResponse>builder()
                .statusCode(200).serviceName(SERVICE)
                .message("Comprobante recibido. Un administrador lo validará pronto.")
                .response(data).build());
    }

    /**
     * Stream del binario. Se hace cacheable (immutable) porque la key incluye
     * un UUID — nunca cambiará el contenido.
     */
    @GetMapping("/api/public/comprobantes/stream")
    public ResponseEntity<ByteArrayResource> stream(@RequestParam("key") String key) {
        ComprobanteStorageService.Descarga d = storage.descargar(key);
        ByteArrayResource body = new ByteArrayResource(d.bytes());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        d.mime() == null ? "application/octet-stream" : d.mime()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"comprobante\"")
                .body(body);
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Header X-User-Id ausente");
        }
        try { return Long.valueOf(header); }
        catch (NumberFormatException ex) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Header X-User-Id inválido");
        }
    }
}
