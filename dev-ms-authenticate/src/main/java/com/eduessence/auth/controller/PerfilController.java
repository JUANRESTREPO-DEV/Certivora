package com.eduessence.auth.controller;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.ActualizarPerfilRequest;
import com.eduessence.auth.model.dto.CambiarPasswordRequest;
import com.eduessence.auth.model.dto.GeneralResponseDTO;
import com.eduessence.auth.model.dto.PerfilResponse;
import com.eduessence.auth.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Perfil", description = "Lectura y actualización del perfil del usuario")
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private static final String SERVICE = "authenticate-service";

    private final PerfilService perfilService;

    @Operation(summary = "Obtener mi perfil")
    @GetMapping
    public ResponseEntity<GeneralResponseDTO<PerfilResponse>> miPerfil(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = parseUserId(userIdHeader);
        PerfilResponse data = perfilService.obtener(userId);
        return ResponseEntity.ok(GeneralResponseDTO.<PerfilResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").response(data).build());
    }

    @Operation(summary = "Actualizar mi perfil")
    @PutMapping
    public ResponseEntity<GeneralResponseDTO<PerfilResponse>> actualizar(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody ActualizarPerfilRequest request
    ) {
        Long userId = parseUserId(userIdHeader);
        PerfilResponse data = perfilService.actualizar(userId, request);
        return ResponseEntity.ok(GeneralResponseDTO.<PerfilResponse>builder()
                .statusCode(200).serviceName(SERVICE).message("Perfil actualizado").response(data).build());
    }

    @Operation(summary = "Cambiar mi contraseña")
    @PostMapping("/cambiar-password")
    public ResponseEntity<GeneralResponseDTO<Void>> cambiarPassword(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CambiarPasswordRequest request
    ) {
        Long userId = parseUserId(userIdHeader);
        perfilService.cambiarPassword(userId, request);
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200).serviceName(SERVICE).message("Contraseña actualizada")
                .response(null).build());
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
