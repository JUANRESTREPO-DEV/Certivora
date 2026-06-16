package com.eduessence.auth.controller;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.*;
import com.eduessence.auth.service.PermissionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Permisos", description = "Sidebar dinámico, permisos efectivos y administración de overrides")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionsController {

    private static final String SERVICE = "authenticate-service";

    private final PermissionsService permissionsService;

    /* ───────────────────── self (usuario logueado) ───────────────────── */

    @Operation(summary = "Sidebar visible para el usuario logueado")
    @GetMapping("/sidebar")
    public ResponseEntity<GeneralResponseDTO<List<ModuloDef>>> sidebar(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        List<ModuloDef> data = permissionsService.sidebar(parseUserId(userIdHeader));
        return ok(data, "OK");
    }

    @Operation(summary = "Permisos efectivos del usuario logueado")
    @GetMapping("/me")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> miPermisos(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        EffectivePermissions data = permissionsService.efectivosDe(parseUserId(userIdHeader));
        return ok(data, "OK");
    }

    @Operation(summary = "Acciones con estado calculado para el usuario logueado")
    @GetMapping("/actions/me")
    public ResponseEntity<GeneralResponseDTO<List<AccionDef>>> misAcciones(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        List<AccionDef> data = permissionsService.accionesDe(parseUserId(userIdHeader));
        return ok(data, "OK");
    }

    /* ───────────────────── admin sobre otros usuarios ───────────────────── */

    @Operation(summary = "Permisos efectivos de otro usuario (admin)")
    @GetMapping("/user/{id}/effective")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> efectivosDe(@PathVariable Long id) {
        return ok(permissionsService.efectivosDe(id), "OK");
    }

    @Operation(summary = "Acciones con estado calculado para otro usuario (admin)")
    @GetMapping("/actions/{id}")
    public ResponseEntity<GeneralResponseDTO<List<AccionDef>>> accionesDe(@PathVariable Long id) {
        return ok(permissionsService.accionesDe(id), "OK");
    }

    @Operation(summary = "ALLOW / DENY individual de un permiso a un usuario")
    @PatchMapping("/user/{id}/permiso/{codigoPermiso}")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> override(
            @PathVariable Long id,
            @PathVariable String codigoPermiso,
            @Valid @RequestBody OverrideRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String adminIdHeader
    ) {
        Long adminId = adminIdHeader == null || adminIdHeader.isBlank() ? null : parseUserId(adminIdHeader);
        EffectivePermissions data = permissionsService.aplicarOverride(id, codigoPermiso, body.getTipo().name(), adminId);
        return ok(data, "Override aplicado");
    }

    @Operation(summary = "Quita el override individual (vuelve a heredar del rol)")
    @DeleteMapping("/user/{id}/permiso/{codigoPermiso}")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> quitarOverride(
            @PathVariable Long id,
            @PathVariable String codigoPermiso
    ) {
        return ok(permissionsService.quitarOverride(id, codigoPermiso), "Override eliminado");
    }

    @Operation(summary = "GRANT / REVOKE atómico de todos los permisos requeridos de una acción")
    @PatchMapping("/user/{id}/action-bundle/{idAccion}")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> bundle(
            @PathVariable Long id,
            @PathVariable Long idAccion,
            @Valid @RequestBody BundleRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String adminIdHeader
    ) {
        Long adminId = adminIdHeader == null || adminIdHeader.isBlank() ? null : parseUserId(adminIdHeader);
        EffectivePermissions data = permissionsService.aplicarBundle(id, idAccion, body.getTipo(), adminId);
        return ok(data, "Bundle aplicado");
    }

    @Operation(summary = "Elimina todos los overrides del usuario (vuelve completamente al rol)")
    @PostMapping("/user/{id}/reset-to-role")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> resetearARol(@PathVariable Long id) {
        return ok(permissionsService.resetearARol(id), "Permisos reseteados al rol");
    }

    @Operation(summary = "Copia los overrides de otro usuario al destino")
    @PostMapping("/user/{id}/copy-from/{idOrigen}")
    public ResponseEntity<GeneralResponseDTO<EffectivePermissions>> copiar(
            @PathVariable Long id,
            @PathVariable Long idOrigen,
            @RequestHeader(value = "X-User-Id", required = false) String adminIdHeader
    ) {
        Long adminId = adminIdHeader == null || adminIdHeader.isBlank() ? null : parseUserId(adminIdHeader);
        return ok(permissionsService.copiarDesde(id, idOrigen, adminId), "Permisos copiados");
    }

    /* ───────────────────── helpers ───────────────────── */

    private <T> ResponseEntity<GeneralResponseDTO<T>> ok(T data, String mensaje) {
        return ResponseEntity.ok(GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(mensaje).response(data).build());
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new AuthApiException(ServerApiStatusCode.TOKEN_INVALIDO, "Header X-User-Id ausente");
        }
        try {
            return Long.valueOf(header);
        } catch (NumberFormatException ex) {
            throw new AuthApiException(ServerApiStatusCode.TOKEN_INVALIDO, "Header X-User-Id inválido");
        }
    }
}
