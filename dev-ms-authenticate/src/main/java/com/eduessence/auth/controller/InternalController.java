package com.eduessence.auth.controller;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.GeneralResponseDTO;
import com.eduessence.auth.model.dto.PerfilResponse;
import com.eduessence.auth.model.dto.TokenValidacionResponse;
import com.eduessence.auth.model.entity.Persona;
import com.eduessence.auth.model.entity.Usuario;
import com.eduessence.auth.model.enums.EstadoUsuario;
import com.eduessence.auth.repository.UsuarioRepository;
import com.eduessence.auth.service.AuthService;
import com.eduessence.auth.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints exclusivos para comunicación entre microservicios.
 * No pasan por la validación JWT del gateway (están en public-paths).
 */
@Tag(name = "Internal", description = "Endpoints internos para otros microservicios")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String SERVICE = "authenticate-service";

    private final AuthService authService;
    private final PerfilService perfilService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Validar JWT (usado por otros micros)")
    @GetMapping("/verificar-token")
    public ResponseEntity<GeneralResponseDTO<TokenValidacionResponse>> verificar(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthApiException(ServerApiStatusCode.TOKEN_INVALIDO);
        }
        String token = authHeader.substring(7).trim();
        TokenValidacionResponse data = authService.verificarToken(token);
        return ResponseEntity.ok(GeneralResponseDTO.<TokenValidacionResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    @Operation(summary = "Obtener perfil completo de un usuario por id")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<GeneralResponseDTO<PerfilResponse>> perfilPorId(@PathVariable Long id) {
        PerfilResponse data = perfilService.obtener(id);
        return ResponseEntity.ok(GeneralResponseDTO.<PerfilResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    /**
     * Listado paginado de usuarios en estado ACTIVO, con datos mínimos para
     * broadcasts (broadcasts de correo desde otros micros).
     * <p>Respuesta: {@code { statusCode, response: { total, totalPages,
     * page, size, items: [{id, email, nombres, apellidos}] }}}
     */
    @Operation(summary = "Listado paginado de usuarios activos (para broadcasts internos)")
    @GetMapping("/usuarios/activos")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> listarActivos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 500);
        Page<Usuario> pg = usuarioRepository.findAllByEstado(
                EstadoUsuario.ACTIVO,
                PageRequest.of(page, safeSize, Sort.by("id").ascending())
        );

        List<Map<String, Object>> items = pg.getContent().stream()
                .map(u -> {
                    Persona p = u.getPersona();
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("email", u.getEmail());
                    m.put("nombres", p == null ? "" : (p.getNombres() == null ? "" : p.getNombres()));
                    m.put("apellidos", p == null ? "" : (p.getApellidos() == null ? "" : p.getApellidos()));
                    return m;
                })
                .toList();

        Map<String, Object> data = Map.of(
                "total", pg.getTotalElements(),
                "totalPages", pg.getTotalPages(),
                "page", pg.getNumber(),
                "size", pg.getSize(),
                "items", items
        );
        return ResponseEntity.ok(GeneralResponseDTO.<Map<String, Object>>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }
}
