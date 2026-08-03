package com.eduessence.auth.controller;

import com.eduessence.auth.feign.CertificadosServiceClient;
import com.eduessence.auth.feign.CursosServiceClient;
import com.eduessence.auth.feign.PagosServiceClient;
import com.eduessence.auth.model.dto.ActualizarUsuarioRequest;
import com.eduessence.auth.model.dto.CrearUsuarioRequest;
import com.eduessence.auth.model.dto.GeneralResponseDTO;
import com.eduessence.auth.model.dto.RolResponse;
import com.eduessence.auth.model.dto.UsuarioDetalleResponse;
import com.eduessence.auth.model.dto.UsuarioListResponse;
import com.eduessence.auth.service.UsuariosAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestión de usuarios para el back-office (/app/usuarios).
 *
 * Todos los endpoints son admin — el gateway/guard filtra por
 * {@code USUARIO_VER}, {@code USUARIO_EDITAR}, {@code USUARIO_ROL}.
 */
@Tag(name = "Usuarios (admin)")
@RestController
@RequestMapping("/api/usuarios-admin")
@RequiredArgsConstructor
public class UsuariosAdminController {

    private static final String SERVICE = "authenticate-service";

    private final UsuariosAdminService usuariosService;
    private final CursosServiceClient cursosClient;
    private final PagosServiceClient pagosClient;
    private final CertificadosServiceClient certificadosClient;

    /* ─────────────────── Listado / detalle ─────────────────── */

    @Operation(summary = "Listado paginado de usuarios con filtros")
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<UsuarioListResponse> data = usuariosService.listar(q, estado, rol, pageable);
        Map<String, Object> body = Map.of(
                "content", data.getContent(),
                "totalElements", data.getTotalElements(),
                "totalPages", data.getTotalPages(),
                "page", data.getNumber(),
                "size", data.getSize()
        );
        return ResponseEntity.ok(ok(body, "OK"));
    }

    @Operation(summary = "Detalle de un usuario")
    @GetMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<UsuarioDetalleResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ok(usuariosService.obtener(id), "OK"));
    }

    /* ─────────────────── Crear / editar ─────────────────── */

    @Operation(summary = "Crear un usuario")
    @PostMapping
    public ResponseEntity<GeneralResponseDTO<UsuarioDetalleResponse>> crear(
            @Valid @RequestBody CrearUsuarioRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader) {
        Long actorId = parseLong(actorHeader);
        return ResponseEntity.status(201).body(ok(
                usuariosService.crear(req, actorId), "Usuario creado"));
    }

    @Operation(summary = "Actualizar un usuario (patch parcial)")
    @PutMapping("/{id}")
    public ResponseEntity<GeneralResponseDTO<UsuarioDetalleResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader) {
        Long actorId = parseLong(actorHeader);
        return ResponseEntity.ok(ok(usuariosService.actualizar(id, req, actorId), "Usuario actualizado"));
    }

    /* ─────────────────── Estado / roles / reset ─────────────────── */

    @Operation(summary = "Cambiar el estado del usuario (ACTIVO/INACTIVO/BLOQUEADO)")
    @PostMapping("/{id}/estado")
    public ResponseEntity<GeneralResponseDTO<UsuarioDetalleResponse>> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader) {
        String estado = body.getOrDefault("estado", "").trim();
        return ResponseEntity.ok(ok(
                usuariosService.cambiarEstado(id, estado, parseLong(actorHeader)),
                "Estado actualizado"));
    }

    @Operation(summary = "Envía un correo de reset de contraseña al usuario")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<GeneralResponseDTO<Void>> forzarReset(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader) {
        usuariosService.solicitarResetPassword(id, parseLong(actorHeader));
        return ResponseEntity.ok(ok(null, "Correo de recuperación enviado"));
    }

    @Operation(summary = "Reemplazar los roles del usuario")
    @PutMapping("/{id}/roles")
    public ResponseEntity<GeneralResponseDTO<UsuarioDetalleResponse>> actualizarRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader) {
        List<String> roles = body.getOrDefault("roles", List.of());
        return ResponseEntity.ok(ok(
                usuariosService.actualizarRoles(id, roles, parseLong(actorHeader)),
                "Roles actualizados"));
    }

    /* ─────────────────── Roles disponibles ─────────────────── */

    @Operation(summary = "Roles disponibles en el sistema con conteo de usuarios")
    @GetMapping("/roles")
    public ResponseEntity<GeneralResponseDTO<List<RolResponse>>> listarRoles() {
        return ResponseEntity.ok(ok(usuariosService.listarRoles(), "OK"));
    }

    /* ─────────────────── Historial (Feign) ─────────────────── */

    @Operation(summary = "Matrículas del usuario (vía dev-ms-cursos)")
    @GetMapping("/{id}/matriculas")
    public ResponseEntity<GeneralResponseDTO<Object>> historialMatriculas(@PathVariable Long id) {
        Map<String, Object> resp = cursosClient.matriculasPorUsuario(id);
        return ResponseEntity.ok(ok(resp == null ? List.of() : resp.get("response"), "OK"));
    }

    @Operation(summary = "Pagos del usuario (vía dev-ms-pagos)")
    @GetMapping("/{id}/pagos")
    public ResponseEntity<GeneralResponseDTO<Object>> historialPagos(@PathVariable Long id) {
        Map<String, Object> resp = pagosClient.pagosPorUsuario(id);
        return ResponseEntity.ok(ok(resp == null ? List.of() : resp.get("response"), "OK"));
    }

    @Operation(summary = "Certificados del usuario (vía dev-ms-certificados)")
    @GetMapping("/{id}/certificados")
    public ResponseEntity<GeneralResponseDTO<Object>> historialCertificados(@PathVariable Long id) {
        Map<String, Object> resp = certificadosClient.certificadosPorUsuario(id);
        return ResponseEntity.ok(ok(resp == null ? List.of() : resp.get("response"), "OK"));
    }

    /* ─────────────────── helpers ─────────────────── */

    private static <T> GeneralResponseDTO<T> ok(T data, String msg) {
        return GeneralResponseDTO.<T>builder()
                .statusCode(200).serviceName(SERVICE).message(msg).response(data).build();
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
