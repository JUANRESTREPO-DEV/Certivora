package com.eduessence.auth.service.impl;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.feign.SendmailServiceClient;
import com.eduessence.auth.jwt.TokenService;
import com.eduessence.auth.model.dto.LoginRequest;
import com.eduessence.auth.model.dto.LoginResponse;
import com.eduessence.auth.model.dto.RecuperarPasswordRequest;
import com.eduessence.auth.model.dto.RegisterRequest;
import com.eduessence.auth.model.dto.RestablecerPasswordRequest;
import com.eduessence.auth.model.dto.TokenValidacionResponse;
import com.eduessence.auth.model.entity.AuditLog;
import com.eduessence.auth.model.entity.PasswordResetToken;
import com.eduessence.auth.model.entity.Persona;
import com.eduessence.auth.model.entity.Rol;
import com.eduessence.auth.model.entity.Usuario;
import com.eduessence.auth.model.enums.EstadoUsuario;
import com.eduessence.auth.repository.AuditLogRepository;
import com.eduessence.auth.repository.PasswordResetTokenRepository;
import com.eduessence.auth.repository.PersonaRepository;
import com.eduessence.auth.repository.RolRepository;
import com.eduessence.auth.repository.UsuarioRepository;
import com.eduessence.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lógica de autenticación.
 *
 * Reglas implementadas:
 *  - R-AUTH-01 Email único normalizado lowercase
 *  - R-AUTH-02 Password mín 8 chars con número + mayúscula (validado en DTO)
 *  - R-AUTH-03 Bloqueo de cuenta tras 5 intentos fallidos por 15 min
 *  - R-AUTH-04 JWT vigente 1 h
 *  - R-AUTH-05 Token de recuperación 30 min, un solo uso
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int BLOQUEO_MINUTOS = 15;
    private static final int RESET_TOKEN_MINUTOS = 30;
    private static final String ROL_DEFAULT = "ASISTENTE";

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SendmailServiceClient sendmail;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        String identificador = request.getUsername().trim().toLowerCase();

        // Acepta email o username — si contiene @ es email, si no, username.
        Usuario usuario = (identificador.contains("@")
                ? usuarioRepository.findByEmailIgnoreCase(identificador)
                : usuarioRepository.findByUsernameIgnoreCase(identificador))
                .orElseThrow(() -> {
                    audit(null, ip, userAgent, "LOGIN", "FALLIDO",
                            "Identificador no existe: " + identificador, identificador);
                    return new AuthApiException(ServerApiStatusCode.CREDENCIALES_INVALIDAS);
                });

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            audit(usuario.getId(), ip, userAgent, "LOGIN", "FALLIDO", "Usuario inactivo", identificador);
            throw new AuthApiException(ServerApiStatusCode.USUARIO_INACTIVO);
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            audit(usuario.getId(), ip, userAgent, "LOGIN", "FALLIDO", "Usuario bloqueado", identificador);
            throw new AuthApiException(ServerApiStatusCode.USUARIO_BLOQUEADO);
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= MAX_INTENTOS_FALLIDOS) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));
                log.warn("Usuario {} bloqueado por {} min", identificador, BLOQUEO_MINUTOS);
            }
            usuarioRepository.save(usuario);
            audit(usuario.getId(), ip, userAgent, "LOGIN", "FALLIDO",
                    "Credenciales inválidas. Intentos: " + intentos, identificador);
            throw new AuthApiException(ServerApiStatusCode.CREDENCIALES_INVALIDAS);
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = tokenService.emitir(usuario);
        audit(usuario.getId(), ip, userAgent, "LOGIN", "OK", "Login exitoso", identificador);

        Set<String> roles = usuario.getRoles().stream().map(Rol::getCodigo).collect(Collectors.toSet());
        Set<String> permisos = usuario.getRoles().stream()
                .flatMap(r -> r.getPermisos().stream())
                .map(p -> p.getCodigo())
                .collect(Collectors.toSet());

        Persona persona = usuario.getPersona();

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(tokenService.getExpirationSeconds())
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .nombres(persona != null ? persona.getNombres() : null)
                .apellidos(persona != null ? persona.getApellidos() : null)
                .roles(roles)
                .permisos(permisos)
                .build();
    }

    @Override
    @Transactional
    public Long register(RegisterRequest request, String ip) {
        String email = request.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthApiException(ServerApiStatusCode.EMAIL_DUPLICADO);
        }

        String username = generarUsername(request.getNombres(), request.getApellidos());
        Rol rolDefault = rolRepository.findByCodigo(ROL_DEFAULT)
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.ROL_NO_ENCONTRADO));

        Usuario usuario = Usuario.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .estado(EstadoUsuario.ACTIVO)
                .emailVerificado(false)
                .intentosFallidos(0)
                .roles(Set.of(rolDefault))
                .build();

        Persona persona = Persona.builder()
                .nombres(request.getNombres().trim())
                .apellidos(request.getApellidos().trim())
                .usuario(usuario)
                .build();
        usuario.setPersona(persona);

        usuarioRepository.save(usuario);
        audit(usuario.getId(), ip, null, "REGISTER", "OK", "Cuenta creada", email);

        try {
            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", "WELCOME",
                    "destinatario", Map.of(
                            "correo", email,
                            "nombre", persona.getNombres()
                    ),
                    "variables", Map.of(
                            "nombreDestinatario", persona.getNombres()
                    )
            ));
        } catch (Exception ex) {
            log.warn("No se pudo enviar email de bienvenida a {}: {}", email, ex.getMessage());
        }

        return usuario.getId();
    }

    @Override
    @Transactional
    public void recuperarPassword(RecuperarPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        // No revelar si el email existe o no (anti-enumeración)
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            String token = generarToken();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .usuario(usuario)
                    .token(token)
                    .expira(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTOS))
                    .usado(false)
                    .build();
            resetTokenRepository.save(prt);
            audit(usuario.getId(), null, null, "RESET_PASSWORD_SOLICITADO", "OK", "Token generado", email);

            try {
                String nombre = usuario.getPersona() != null ? usuario.getPersona().getNombres() : "";
                sendmail.enviarEmail(Map.of(
                        "nombreTemplate", "RESET_PASSWORD",
                        "destinatario", Map.of(
                                "correo", email,
                                "nombre", nombre
                        ),
                        "variables", Map.of(
                                "token", token,
                                "minutos", RESET_TOKEN_MINUTOS
                        )
                ));
            } catch (Exception ex) {
                log.warn("No se pudo enviar email de recuperación a {}: {}", email, ex.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest request) {
        PasswordResetToken prt = resetTokenRepository.findByTokenAndUsadoFalse(request.getToken())
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.TOKEN_RESET_INVALIDO));

        if (prt.getExpira().isBefore(LocalDateTime.now())) {
            prt.setUsado(true);
            resetTokenRepository.save(prt);
            throw new AuthApiException(ServerApiStatusCode.TOKEN_RESET_INVALIDO);
        }

        Usuario usuario = prt.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        prt.setUsado(true);
        resetTokenRepository.save(prt);

        audit(usuario.getId(), null, null, "RESET_PASSWORD_OK", "OK",
                "Contraseña cambiada", usuario.getEmail());
    }

    @Override
    public void logout(String token) {
        tokenService.revocar(token);
    }

    @Override
    public TokenValidacionResponse verificarToken(String token) {
        Claims claims = tokenService.validar(token);
        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        @SuppressWarnings("unchecked")
        Set<String> roles = Set.copyOf((java.util.Collection<String>) claims.get("roles"));
        long expSeg = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

        return TokenValidacionResponse.builder()
                .valido(true)
                .userId(userId)
                .email(email)
                .roles(roles)
                .expiraEnSegundos(expSeg)
                .build();
    }

    private String generarUsername(String nombres, String apellidos) {
        String base = (nombres.trim().substring(0, Math.min(2, nombres.trim().length())) +
                apellidos.trim().replaceAll("\\s+", ""))
                .toLowerCase();
        String candidato = base;
        int sufijo = 1;
        while (usuarioRepository.existsByUsernameIgnoreCase(candidato)) {
            candidato = base + sufijo++;
        }
        return candidato;
    }

    private String generarToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void audit(Long usuarioId, String ip, String userAgent,
                       String accion, String resultado, String detalle, String recurso) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .usuarioId(usuarioId)
                    .ip(ip)
                    .userAgent(userAgent)
                    .accion(accion)
                    .resultado(resultado)
                    .detalle(detalle)
                    .recurso(recurso)
                    .build());
        } catch (Exception ex) {
            log.warn("No se pudo registrar audit: {}", ex.getMessage());
        }
    }
}
