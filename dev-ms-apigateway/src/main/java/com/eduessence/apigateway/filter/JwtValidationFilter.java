package com.eduessence.apigateway.filter;

import com.eduessence.apigateway.config.PublicRoutesProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Valida el JWT en el header Authorization para rutas privadas.
 *
 * - Rutas listadas en {@link PublicRoutesProperties} pasan sin validación.
 * - Si el token es válido, inyecta headers internos {@code X-User-Id},
 *   {@code X-User-Email}, {@code X-User-Roles} para que los micros downstream
 *   confíen en el gateway y no tengan que re-decodificar el JWT.
 * - Si falta o es inválido, devuelve 401.
 *
 * También garantiza un {@code X-Request-Id} para trazabilidad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicRoutesProperties publicRoutes;

    @Value("${eduessence.jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        String requestId = request.getHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String finalRequestId = requestId;

        // CORS preflight: nunca trae Authorization, debe pasar para que el
        // CorsWebFilter le ponga los headers correctos.
        if (HttpMethod.OPTIONS.equals(method)) {
            ServerHttpRequest enriched = request.mutate()
                    .header("X-Request-Id", finalRequestId)
                    .build();
            return chain.filter(exchange.mutate().request(enriched).build());
        }

        if (publicRoutes.matches(path)) {
            ServerHttpRequest enriched = request.mutate()
                    .header("X-Request-Id", finalRequestId)
                    .build();
            return chain.filter(exchange.mutate().request(enriched).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange.getResponse(), "Token ausente o malformado");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            Object rolesClaim = claims.get("roles");
            String roles = rolesClaim == null ? "" : rolesClaim.toString();

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", userId == null ? "" : userId)
                    .header("X-User-Email", email == null ? "" : email)
                    .header("X-User-Roles", roles)
                    .header("X-Request-Id", finalRequestId)
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException ex) {
            log.warn("[{}] JWT inválido en {}: {}", finalRequestId, path, ex.getMessage());
            return unauthorized(exchange.getResponse(), "Token inválido o expirado");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = """
                {"statusCode":401,"serviceName":"apigateway","message":"%s","response":null}
                """.formatted(message);
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
