package com.eduessence.inbox.controller;

import com.eduessence.inbox.common.RequestContext;
import com.eduessence.inbox.model.dto.GeneralResponseDTO;
import com.eduessence.inbox.model.dto.request.ActualizarBuzonRequest;
import com.eduessence.inbox.model.dto.request.CrearAliasRequest;
import com.eduessence.inbox.model.dto.request.CrearBuzonRequest;
import com.eduessence.inbox.model.dto.request.OtorgarAccesoRequest;
import com.eduessence.inbox.model.dto.response.BuzonResponse;
import com.eduessence.inbox.service.BuzonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administración de buzones corporativos.
 *
 * R-BUZ-03: crear / actualizar / eliminar / dar acceso → solo ADMIN o GERENTE.
 * Listar y obtener requieren JWT pero se permiten a cualquier usuario autenticado.
 */
@Tag(name = "Buzones (administración)")
@RestController
@RequestMapping("/api/buzones")
@RequiredArgsConstructor
public class BuzonController {

    private static final String SERVICE = "inbox-service";
    private final BuzonService buzonService;
    private final RequestContext ctx;

    @PostMapping
    public ResponseEntity<GeneralResponseDTO<BuzonResponse>> crear(@Valid @RequestBody CrearBuzonRequest req) {
        ctx.requireAdminOGerente();
        BuzonResponse data = buzonService.crear(req, ctx.usuarioId());
        return ResponseEntity.status(201).body(ok(data, "Buzón creado", 201));
    }

    @GetMapping
    public ResponseEntity<GeneralResponseDTO<Page<BuzonResponse>>> listar(
            @RequestParam(required = false) Boolean soloActivos, Pageable pageable) {
        return ResponseEntity.ok(ok(buzonService.listar(soloActivos, pageable), "OK", 200));
    }

    @GetMapping("/mios")
    public ResponseEntity<GeneralResponseDTO<List<BuzonResponse>>> mios() {
        return ResponseEntity.ok(ok(buzonService.misBuzones(ctx.usuarioId()), "OK", 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<BuzonResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(buzonService.obtener(id), "OK", 200));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<BuzonResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarBuzonRequest req) {
        ctx.requireAdminOGerente();
        return ResponseEntity.ok(ok(buzonService.actualizar(id, req), "Buzón actualizado", 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<Void>> eliminar(@PathVariable Long id) {
        ctx.requireAdminOGerente();
        buzonService.eliminar(id);
        return ResponseEntity.ok(ok(null, "Buzón desactivado", 200));
    }

    /* ── alias ── */

    @PostMapping("/alias")
    public ResponseEntity<GeneralResponseDTO<BuzonResponse>> crearAlias(@Valid @RequestBody CrearAliasRequest req) {
        ctx.requireAdminOGerente();
        return ResponseEntity.status(201).body(ok(buzonService.crearAlias(req), "Alias creado", 201));
    }

    @DeleteMapping("/alias/{aliasId}")
    public ResponseEntity<GeneralResponseDTO<Void>> eliminarAlias(@PathVariable Long aliasId) {
        ctx.requireAdminOGerente();
        buzonService.eliminarAlias(aliasId);
        return ResponseEntity.ok(ok(null, "Alias eliminado", 200));
    }

    /* ── accesos ── */

    @PostMapping("/{id}/accesos")
    public ResponseEntity<GeneralResponseDTO<BuzonResponse>> otorgarAcceso(
            @PathVariable Long id, @Valid @RequestBody OtorgarAccesoRequest req) {
        ctx.requireAdminOGerente();
        return ResponseEntity.ok(ok(buzonService.otorgarAcceso(id, req, ctx.usuarioId()),
                "Acceso otorgado", 200));
    }

    @DeleteMapping("/{id}/accesos/{usuarioId}")
    public ResponseEntity<GeneralResponseDTO<Void>> revocarAcceso(
            @PathVariable Long id, @PathVariable Long usuarioId) {
        ctx.requireAdminOGerente();
        buzonService.revocarAcceso(id, usuarioId);
        return ResponseEntity.ok(ok(null, "Acceso revocado", 200));
    }

    private static <T> GeneralResponseDTO<T> ok(T data, String msg, int status) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(status).serviceName(SERVICE).message(msg).response(data).build();
    }
}
