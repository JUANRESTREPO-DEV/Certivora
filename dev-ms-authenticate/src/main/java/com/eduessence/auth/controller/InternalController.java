package com.eduessence.auth.controller;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.GeneralResponseDTO;
import com.eduessence.auth.model.dto.PerfilResponse;
import com.eduessence.auth.model.dto.TokenValidacionResponse;
import com.eduessence.auth.service.AuthService;
import com.eduessence.auth.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
