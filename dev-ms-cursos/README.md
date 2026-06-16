# dev-ms-cursos

Catálogo de cursos, matrículas, progreso, exámenes, actividades y conectividad para Eduessence.

## Modalidades soportadas

Un curso tiene **1..N modalidades** (`curso_modalidad`):

| Modalidad | Descripción | Requiere |
|---|---|---|
| `PRESENCIAL` | Clase física | `sede`, `cupo_modalidad` |
| `VIRTUAL_LIVE` | Streaming en vivo (OBS → dev-ms-streaming) | `sesion_virtual` |
| `GRABADO` | Videos pregrabados en S3 | `seccion` + `leccion` |

**HIBRIDO** = curso con ≥2 modalidades activas. La matrícula del usuario
elige cuáles sigue (`matricula_modalidad`).

## Trazabilidad (paso a paso)

```
curso → seccion[]          ← bloqueante (sí/no)
        └ leccion[]        ← tipo + bloqueante
                ├ VIDEO          (recurso_url, duracion_segundos)
                ├ TEXTO          (contenido_texto)
                ├ PDF            (recurso_url)
                ├ EXAMEN         (examen_id  → examen + preguntas + opciones)
                ├ ACTIVIDAD      (actividad_id → entrega_actividad calificada)
                └ SESION_VIRTUAL (sesion_virtual_id → stream del dev-ms-streaming)
```

El usuario va completando lecciones. Cuando una lección es `bloqueante=true`,
no puede pasar a la siguiente hasta superarla.

## Aprobación parametrizable

Por curso (`configuracion_aprobacion`):
- `nota_minima_general` — default 70 (cae como fallback si la actividad/examen no define propia)
- `progreso_minimo_pct` — default 80
- `presencia_minima_pct` — default 70 (solo aplica si la matrícula incluye `VIRTUAL_LIVE`)

Por examen/actividad: `nota_minima_propia` que sobreescribe el general.

El servicio `AprobacionService.evaluar(matriculaId)` calcula los 4 criterios
y, si todo se cumple, marca la matrícula como `APROBADA` y dispara la emisión
del certificado (vía Feign a `dev-ms-certificados`).

## Log de conectividad

Para `VIRTUAL_LIVE`, el front del viewer envía heartbeats a
`dev-ms-streaming /viewer-heartbeat` cada 30 s. Streaming reenvía
agregados periódicos a `dev-ms-cursos /internal/asistencia-virtual/heartbeat`
y aquí se actualiza `asistencia_virtual.minutos_conectado` y
`porcentaje_presencia` = (minutos_conectado × 100) / duracion_sesion.

## Endpoints planificados

### Públicos
- `GET /api/public/cursos` — listado activo
- `GET /api/public/cursos/{slug}` — detalle

### Privados
- `POST /api/cursos/{id}/inscribir` — crear matrícula (verifica cupo + modalidades)
- `GET /api/matriculas/me` — mis matrículas
- `PUT /api/progreso/leccion/{leccionId}` — actualizar segundos vistos
- `POST /api/examenes/{id}/intento` — iniciar intento
- `POST /api/intentos/{id}/finalizar` — enviar respuestas
- `POST /api/actividades/{id}/entregar` — subir entrega
- `POST /api/matriculas/{id}/evaluar-aprobacion` — calcular y devolver resultado

### Internos
- `POST /internal/asistencia-virtual/heartbeat` — agregado de presencia
- `GET /internal/cursos/{id}` — datos básicos para otros MS
- `GET /internal/matriculas/{id}/aprobada` — para dev-ms-certificados

## Stack y configuración

- Java 21 · Spring Boot 3.4.5 · Spring Cloud 2024.0.0
- MySQL 8 (RDS) · Flyway
- Variables: lee del `.env` raíz vía `SERVER_PORT_CURSOS=8085`, `SERVER_SERVLET_CONTEXT_PATH_CURSOS=/cursos`, `DBDATABASE`, `RDS_USER`, `RDS_PASSWORD`, `EUREKA_URL`.

## Estado actual del scaffold

Listo: entidades JPA completas, repositorios, `AprobacionService` con el cálculo de los 4 criterios, exceptions handler, configs (Security, OpenAPI, properties multi-perfil), migración Flyway V1.

**Próximo paso**: agregar controllers (Curso, Matrícula, Examen, Intento), services CRUD, integración Feign con `dev-ms-pagos` para validar pago y `dev-ms-streaming` para sesiones virtuales.
