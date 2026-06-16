package com.eduessence.auth.controller;

import com.eduessence.auth.model.dto.GeneralResponseDTO;
import com.eduessence.auth.model.dto.UsuarioBasicoResponse;
import com.eduessence.auth.model.entity.Persona;
import com.eduessence.auth.model.entity.Usuario;
import com.eduessence.auth.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * Búsqueda y lookup de usuarios. Pensado para pickers / autocompletes de
 * otros microservicios y del front (back-office).
 *
 * Devuelve {@link UsuarioBasicoResponse} (id + nombres + apellidos + email).
 */
@Tag(name = "Usuarios (lookup)")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private static final String SERVICE = "authenticate-service";
    private static final int MAX_RESULTS = 50;

    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Busca usuarios por nombre, apellido, username o email")
    @GetMapping("/buscar")
    public ResponseEntity<GeneralResponseDTO<List<UsuarioBasicoResponse>>> buscar(
            @RequestParam(name = "q", required = false, defaultValue = "") String q) {

        String filtro = q.trim().toLowerCase();
        List<UsuarioBasicoResponse> data = usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() != null && u.getEstado().name().equals("ACTIVO"))
                .filter(u -> filtro.isEmpty() || matches(u, filtro))
                .sorted(Comparator.comparing(u -> {
                    Persona p = u.getPersona();
                    return p == null ? u.getUsername() : (p.getNombres() + " " + p.getApellidos());
                }, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_RESULTS)
                .map(UsuariosController::toBasic)
                .toList();

        return ResponseEntity.ok(GeneralResponseDTO.<List<UsuarioBasicoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    @Operation(summary = "Obtiene varios usuarios por ID (lookup batch)")
    @GetMapping("/lookup")
    public ResponseEntity<GeneralResponseDTO<List<UsuarioBasicoResponse>>> lookup(
            @RequestParam(name = "ids") List<Long> ids) {

        List<UsuarioBasicoResponse> data = usuarioRepository.findAllById(ids).stream()
                .map(UsuariosController::toBasic)
                .toList();

        return ResponseEntity.ok(GeneralResponseDTO.<List<UsuarioBasicoResponse>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    /* ──────────────── helpers ──────────────── */

    private static boolean matches(Usuario u, String q) {
        if (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) return true;
        if (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)) return true;
        Persona p = u.getPersona();
        if (p == null) return false;
        if (p.getNombres() != null && p.getNombres().toLowerCase().contains(q)) return true;
        if (p.getApellidos() != null && p.getApellidos().toLowerCase().contains(q)) return true;
        String full = (p.getNombres() == null ? "" : p.getNombres()) + " " +
                      (p.getApellidos() == null ? "" : p.getApellidos());
        return full.toLowerCase().contains(q);
    }

    private static UsuarioBasicoResponse toBasic(Usuario u) {
        Persona p = u.getPersona();
        return UsuarioBasicoResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .nombres(p == null ? null : p.getNombres())
                .apellidos(p == null ? null : p.getApellidos())
                .build();
    }
}
