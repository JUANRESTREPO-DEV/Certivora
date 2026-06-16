package com.eduessence.auth.controller;

import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.GeneralResponseDTO;
import com.eduessence.auth.model.dto.LoginRequest;
import com.eduessence.auth.model.dto.LoginResponse;
import com.eduessence.auth.model.dto.RecuperarPasswordRequest;
import com.eduessence.auth.model.dto.RegisterRequest;
import com.eduessence.auth.model.dto.RestablecerPasswordRequest;
import com.eduessence.auth.service.AuthService;
import com.eduessence.auth.utils.HttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "Login, registro y recuperación de contraseña")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SERVICE = "authenticate-service";

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión y obtener JWT")
    @PostMapping("/login")
    public ResponseEntity<GeneralResponseDTO<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http
    ) {
        LoginResponse data = authService.login(request, HttpUtils.clientIp(http), HttpUtils.userAgent(http));
        return ResponseEntity.ok(GeneralResponseDTO.<LoginResponse>builder()
                .statusCode(200)
                .serviceName(SERVICE)
                .message("Login exitoso")
                .response(data)
                .build());
    }

    @Operation(summary = "Registrar nuevo usuario")
    @PostMapping("/register")
    public ResponseEntity<GeneralResponseDTO<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest http
    ) {
        Long id = authService.register(request, HttpUtils.clientIp(http));
        return ResponseEntity.status(201)
                .header("X-Status-Code", ServerApiStatusCode.OK.getCodigo())
                .body(GeneralResponseDTO.<Map<String, Object>>builder()
                        .statusCode(201)
                        .serviceName(SERVICE)
                        .message("Cuenta creada exitosamente")
                        .response(Map.of("usuarioId", id))
                        .build());
    }

    @Operation(summary = "Solicitar recuperación de contraseña (envía email con token)")
    @PostMapping("/recuperar-password")
    public ResponseEntity<GeneralResponseDTO<Void>> recuperarPassword(
            @Valid @RequestBody RecuperarPasswordRequest request
    ) {
        authService.recuperarPassword(request);
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200)
                .serviceName(SERVICE)
                .message("Si el correo está registrado, recibirás un enlace en breve")
                .build());
    }

    @Operation(summary = "Restablecer contraseña con el token recibido")
    @PostMapping("/restablecer-password")
    public ResponseEntity<GeneralResponseDTO<Void>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest request
    ) {
        authService.restablecerPassword(request);
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200)
                .serviceName(SERVICE)
                .message("Contraseña actualizada exitosamente")
                .build());
    }

    @Operation(summary = "Cerrar sesión (revoca el JWT)")
    @PostMapping("/cerrar-sesion")
    public ResponseEntity<GeneralResponseDTO<Void>> cerrarSesion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7).trim()
                : null;
        authService.logout(token);
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200)
                .serviceName(SERVICE)
                .message("Sesión cerrada")
                .build());
    }
}
