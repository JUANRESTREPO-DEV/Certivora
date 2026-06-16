# dev-ms-apigateway

API Gateway de Eduessence basado en **Spring Cloud Gateway** (reactive).

Punto único de entrada para el front. Responsabilidades:
- Enrutamiento por descubrimiento (Eureka) a los micros de negocio.
- Validación de JWT global (filter `JwtValidationFilter`).
- Inyección de headers `X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-Request-Id` a los downstream.
- CORS.
- Circuit breaker (Resilience4j) por ruta.

## Stack

- Java 21
- Spring Boot 3.4.5
- Spring Cloud Gateway 2024.0.0
- JJWT 0.12.6

## Cómo correr

```bash
# Asegúrate de tener dev-ms-eureka corriendo primero
./mvnw spring-boot:run
```

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil activo |
| `SERVER_PORT` | `8080` | Puerto del gateway |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | URL del registry |
| `JWT_SECRET` | — | Secret HMAC SHA-256, **mismo** que `dev-ms-authenticate` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://localhost:4201` | CSV de orígenes |

## Rutas configuradas

| Path | Micro |
|---|---|
| `/auth/**` | AUTHENTICATE-SERVICE |
| `/cursos/**` | CURSOS-SERVICE |
| `/pagos/**` | PAGOS-SERVICE |
| `/certificados/**` | CERTIFICADOS-SERVICE |
| `/eventos/**` | EVENTOS-SERVICE |
| `/sendmail/**` | SENDMAIL-SERVICE |
| `/gestor-documental/**` | GESTOR-DOCUMENTAL-SERVICE |
| `/jobs/**` | JOBS-SERVICE |
| `/chat/**` | CHAT-SERVICE |

## Rutas públicas (sin JWT)

Definidas en `eduessence.security.public-paths` de `application.yml`. Incluyen:
- Login, register, recuperación de contraseña
- `/internal/**` (uso entre micros, no expuesto al front)
- Verificación pública de certificados
- Swagger / Actuator
