package com.eduessence.pagos.controller;

import com.eduessence.pagos.exception.PagosApiException;
import com.eduessence.pagos.exception.ServerApiStatusCode;
import com.eduessence.pagos.model.dto.GeneralResponseDTO;
import com.eduessence.pagos.model.dto.request.CuponRequest;
import com.eduessence.pagos.model.dto.response.CuponResponse;
import com.eduessence.pagos.model.dto.response.CuponUsoResponse;
import com.eduessence.pagos.model.dto.response.ValidarCuponResponse;
import com.eduessence.pagos.service.CuponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Cupones")
@RestController
@RequestMapping("/api/cupones")
@RequiredArgsConstructor
public class CuponController {

    private static final String SERVICE = "pagos-service";
    private final CuponService cuponService;

    /* ─── Validación pública del checkout ─── */

    @GetMapping("/validar")
    public ResponseEntity<GeneralResponseDTO<ValidarCuponResponse>> validar(
            @RequestParam String codigo,
            @RequestParam Long cursoId,
            @RequestParam Integer monto,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long usuarioId = userIdHeader == null || userIdHeader.isBlank()
                ? null : Long.valueOf(userIdHeader);
        ValidarCuponResponse data = cuponService.validar(codigo, usuarioId, cursoId, monto);
        return ResponseEntity.ok(ok(data, "Cupón válido"));
    }

    /* ─── CRUD admin ─── */

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String alcance,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        Page<CuponResponse> resultado = cuponService.listar(search, alcance, activo, pageable);
        Map<String, Object> data = Map.of(
                "items", resultado.getContent(),
                "total", resultado.getTotalElements(),
                "page", resultado.getNumber(),
                "size", resultado.getSize(),
                "totalPages", resultado.getTotalPages()
        );
        return ResponseEntity.ok(ok(data, "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<CuponResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(cuponService.obtener(id), "OK"));
    }

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<CuponResponse>> crear(
            @Valid @RequestBody CuponRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long creadoPor = parseUserIdObligatorio(userIdHeader);
        return ResponseEntity.status(201).body(ok(cuponService.crear(req, creadoPor), "Cupón creado"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<CuponResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody CuponRequest req) {
        return ResponseEntity.ok(ok(cuponService.actualizar(id, req), "Cupón actualizado"));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<GeneralResponseDTO<CuponResponse>> cambiarEstado(
            @PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(ok(cuponService.cambiarEstado(id, activo),
                activo ? "Cupón activado" : "Cupón desactivado"));
    }

    @GetMapping("/{id}/usos")
    public ResponseEntity<GeneralResponseDTO<List<CuponUsoResponse>>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(ok(cuponService.historialUsos(id), "OK"));
    }

    /* ─── helpers ─── */

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }

    private Long parseUserIdObligatorio(String header) {
        if (header == null || header.isBlank()) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "X-User-Id ausente");
        }
        try {
            return Long.valueOf(header);
        } catch (NumberFormatException ex) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "X-User-Id inválido");
        }
    }
}
