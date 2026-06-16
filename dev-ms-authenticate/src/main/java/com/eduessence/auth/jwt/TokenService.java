package com.eduessence.auth.jwt;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Emisión y validación de JWT HS256.
 *
 * Mantiene una blacklist en memoria de tokens revocados (logout). Para
 * múltiples instancias migrar a Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties props;

    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public String emitir(Usuario usuario) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(props.getExpirationMs());

        Set<String> roles = usuario.getRoles().stream()
                .map(r -> r.getCodigo())
                .collect(Collectors.toSet());

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("username", usuario.getUsername())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key())
                .compact();
    }

    public Claims validar(String token) {
        if (blacklist.contains(token)) {
            throw new AuthApiException(ServerApiStatusCode.TOKEN_INVALIDO, "Token revocado");
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key())
                    .requireIssuer(props.getIssuer())
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException ex) {
            log.warn("JWT inválido: {}", ex.getMessage());
            throw new AuthApiException(ServerApiStatusCode.TOKEN_INVALIDO);
        }
    }

    public void revocar(String token) {
        if (token != null && !token.isBlank()) {
            blacklist.add(token);
        }
    }

    public long getExpirationSeconds() {
        return props.getExpirationMs() / 1000;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
