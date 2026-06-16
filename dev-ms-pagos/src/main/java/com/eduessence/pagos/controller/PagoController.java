package com.eduessence.pagos.controller;

import com.eduessence.pagos.exception.PagosApiException;
import com.eduessence.pagos.exception.ServerApiStatusCode;
import com.eduessence.pagos.model.dto.GeneralResponseDTO;
import com.eduessence.pagos.model.dto.request.ConfirmarPagoRequest;
import com.eduessence.pagos.model.dto.request.IniciarPagoRequest;
import com.eduessence.pagos.model.dto.response.IniciarPagoResponse;
import com.eduessence.pagos.model.dto.response.PagoResponse;
import com.eduessence.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Pagos")
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private static final String SERVICE = "pagos-service";
    private final PagoService pagoService;

    @PostMapping("/iniciar")
    public ResponseEntity<GeneralResponseDTO<IniciarPagoResponse>> iniciar(
            @Valid @RequestBody IniciarPagoRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.status(201).body(GeneralResponseDTO.<IniciarPagoResponse>builder()
                .statusCode(201).serviceName(SERVICE).message("Pago iniciado")
                .response(pagoService.iniciar(userId, req)).build());
    }

    @PostMapping("/{id}/confirmar-pago")
    public ResponseEntity<GeneralResponseDTO<PagoResponse>> confirmar(
            @PathVariable Long id, @Valid @RequestBody ConfirmarPagoRequest req) {
        return ResponseEntity.ok(ok(pagoService.confirmarPago(id, req), "Confirmación recibida"));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<GeneralResponseDTO<PagoResponse>> aprobar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long adminId = parseUserId(userIdHeader);
        String obs = body != null && body.get("observacion") != null
                ? body.get("observacion").toString() : null;
        return ResponseEntity.ok(ok(pagoService.aprobar(id, adminId, obs), "Pago aprobado"));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<GeneralResponseDTO<PagoResponse>> rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long adminId = parseUserId(userIdHeader);
        String obs = body != null && body.get("observacion") != null
                ? body.get("observacion").toString() : null;
        return ResponseEntity.ok(ok(pagoService.rechazar(id, adminId, obs), "Pago rechazado"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<PagoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(pagoService.obtener(id), "OK"));
    }

    @GetMapping("/mis-pagos")
    public ResponseEntity<GeneralResponseDTO<List<PagoResponse>>> misPagos(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(GeneralResponseDTO.<List<PagoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(pagoService.misPagos(userId)).build());
    }

    /**
     * Admin: lista los pagos en {@code PENDIENTE_CONFIRMACION} (comprobante
     * subido, esperando aprobación manual). Ordenados por fecha de creación
     * ascendente para que se procesen FIFO.
     */
    @GetMapping("/pendientes-confirmacion")
    public ResponseEntity<GeneralResponseDTO<List<PagoResponse>>> pendientesConfirmacion() {
        return ResponseEntity.ok(GeneralResponseDTO.<List<PagoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK")
                .response(pagoService.listarPendientesConfirmacion()).build());
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Header X-User-Id ausente");
        }
        try { return Long.valueOf(header); }
        catch (NumberFormatException ex) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Header X-User-Id inválido");
        }
    }
}
