# dev-ms-eureka

Service registry de Eduessence basado en **Spring Cloud Netflix Eureka Server**.

## Stack

- Java 21
- Spring Boot 3.4.5
- Spring Cloud 2024.0.0

## Cómo correr

```bash
# Dev (default)
./mvnw spring-boot:run

# QA
SPRING_PROFILES_ACTIVE=qa ./mvnw spring-boot:run

# Prod
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil activo |
| `SERVER_PORT` | `8761` | Puerto de escucha |
| `EUREKA_HOSTNAME` | `localhost` | Hostname al que reportar instancias |

## URLs

- Dashboard: http://localhost:8761
- Health: http://localhost:8761/actuator/health
- Apps registradas: http://localhost:8761/eureka/apps

## Build

```bash
./mvnw clean package
java -jar target/dev-ms-eureka-1.0.0.jar
```
