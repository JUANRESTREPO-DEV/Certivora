# dev-ms-inbox

Bandeja corporativa de Eduessence sobre **AWS SES Inbound**.

- Buzones dinámicos: crear `info@`, `soporte@`, `curso-podologia-2026@` en milisegundos desde la app.
- **Solo ADMIN o GERENTE** pueden crear/editar/eliminar buzones (regla R-BUZ-03).
- Recepción de correos vía SES → SNS → SQS → este MS.
- Vista web propia (Angular) + **forward opcional** a Gmail/Outlook personal del responsable.

---

## Stack

- Spring Boot 3.4.5 · Java 21 · Lombok
- MySQL 8 (RDS) · Flyway
- AWS SDK v2: **SES** (envío) + **S3** (eml/adjuntos) + **SQS** (polling) + **SNS** (alternativa HTTPS)
- Jakarta Mail (`angus-mail`) para parsear MIME y construir respuestas
- OpenFeign hacia `sendmail-service`
- Eureka + SpringDoc

---

## Puerto y contexto

```
SERVER_PORT_INBOX=8093
SERVER_SERVLET_CONTEXT_PATH_INBOX=/inbox
```

---

## Esquema BD (Flyway V1 + V2)

| Tabla              | Notas                                                                         |
|--------------------|-------------------------------------------------------------------------------|
| `buzon`            | Dirección, tipo, `forward_externos` JSON, firma, expira_en                    |
| `buzon_alias`      | Una dirección extra que cae al mismo buzón                                    |
| `buzon_acceso`     | (buzon_id, usuario_id, rol). Permisos finos: LECTOR / RESPONDER / ADMIN_BUZON |
| `mensaje`          | Encabezados + cuerpo + thread + carpeta + estado                              |
| `mensaje_adjunto`  | Adjuntos en bucket S3 separado                                                |

Seed V2: `info@`, `soporte@`, `cursos@`, `pagos@`, `certificados@`, `noreply@` (SOLO_SALIDA), `catchall@` + 4 alias.

---

## Flujo de recepción

```
Internet ── MX ──▶ SES Inbound ── put .eml ──▶ S3 (inbox-raw)
                       │
                       └── notifica ──▶ SNS topic ──▶ SQS queue
                                                          │
                                                  poll @Scheduled cada 5s
                                                          ▼
                                              InboundMessageProcessor
                                              ├── parsea MIME
                                              ├── busca Buzon por To: (exacto → alias → catch-all)
                                              ├── persiste mensaje + adjuntos
                                              └── forward externos via SES (opcional)
```

---

## Endpoints

### Buzones (admin / gerente)

| Método  | Path                                       | Rol mínimo |
|---------|--------------------------------------------|------------|
| POST    | `/api/buzones`                             | ADMIN / GERENTE |
| GET     | `/api/buzones`                             | autenticado |
| GET     | `/api/buzones/mios`                        | autenticado |
| GET     | `/api/buzones/{id}`                        | autenticado |
| PUT     | `/api/buzones/{id}`                        | ADMIN / GERENTE |
| DELETE  | `/api/buzones/{id}`                        | ADMIN / GERENTE |
| POST    | `/api/buzones/alias`                       | ADMIN / GERENTE |
| DELETE  | `/api/buzones/alias/{aliasId}`             | ADMIN / GERENTE |
| POST    | `/api/buzones/{id}/accesos`                | ADMIN / GERENTE |
| DELETE  | `/api/buzones/{id}/accesos/{usuarioId}`    | ADMIN / GERENTE |

### Mensajes (acceso por `buzon_acceso`)

| Método  | Path                                       | Notas |
|---------|--------------------------------------------|-------|
| GET     | `/api/mensajes/buzon/{id}?carpeta=INBOX`   | listar con paginación |
| GET     | `/api/mensajes/buzon/{id}/buscar?q=...`    | búsqueda full-text simple |
| GET     | `/api/mensajes/{id}`                       | detalle + URLs pre-firmadas de adjuntos |
| PATCH   | `/api/mensajes/{id}/leido?valor=true`      | marcar leído / no leído |
| PATCH   | `/api/mensajes/{id}/destacado?valor=true`  | destacar |
| PATCH   | `/api/mensajes/{id}/carpeta?carpeta=SPAM`  | mover a carpeta |
| POST    | `/api/mensajes/{id}/responder`             | RE: + headers In-Reply-To/References |
| POST    | `/api/mensajes/componer`                   | correo nuevo desde un buzón |

### Internal (SNS → MS si no se usa SQS)

| Método  | Path                                       | Notas |
|---------|--------------------------------------------|-------|
| POST    | `/internal/sns/inbound`                    | sin JWT, restringir por SG |

---

## Reglas de autorización

| Acción                          | Verificación                                                |
|---------------------------------|-------------------------------------------------------------|
| Crear/editar/eliminar buzón     | `ctx.requireAdminOGerente()` lee `X-User-Roles`             |
| Otorgar/revocar acceso          | `ctx.requireAdminOGerente()`                                |
| Leer mensajes                   | `BuzonAcceso` con rol ≥ LECTOR, o ADMIN/GERENTE             |
| Responder / mover               | `BuzonAcceso` con rol ≥ RESPONDER, o ADMIN/GERENTE          |
| Procesar inbound (interno)      | público vía `/internal/**` (proteger con SG en AWS)         |

El gateway valida el JWT y mete `X-User-Id`, `X-User-Email`, `X-User-Roles` en cada request. La clase `RequestContext` los lee.

---

## Buckets S3

```
s3://eduessence-inbox-raw-{env}/
   ses-inbound/2026/06/02/<messageId>     ← guardado por la receipt rule de SES

s3://eduessence-inbox-adjuntos-{env}/
   adjuntos/{mensajeId}/{archivo}
```

`inbox-raw` lo configura SES (su receipt rule debe apuntar a este bucket). `inbox-adjuntos` lo escribe este MS y entrega URLs pre-firmadas de 1h.

---

## Variables de entorno

| Var                                     | Ejemplo                                       |
|-----------------------------------------|-----------------------------------------------|
| `SERVER_PORT_INBOX`                     | `8093`                                        |
| `SERVER_SERVLET_CONTEXT_PATH_INBOX`     | `/inbox`                                      |
| `DBDATABASE`                            | `jdbc:mysql://.../eduessence_inbox`           |
| `RDS_USER` / `RDS_PASSWORD`             | credenciales RDS                              |
| `BUCKET_INBOX_RAW`                      | `eduessence-inbox-raw-dev`                    |
| `BUCKET_INBOX_ADJUNTOS`                 | `eduessence-inbox-adjuntos-dev`               |
| `INBOX_DOMINIOS`                        | `eduessence.com,eduessence.co`                |
| `INBOX_FORWARD_FROM`                    | `noreply@eduessence.com`                      |
| `INBOX_SQS_ENABLED`                     | `true` (prod) / `false` (local)               |
| `INBOX_SQS_QUEUE_URL`                   | `https://sqs.us-east-1.amazonaws.com/.../eduessence-inbound-emails-dev` |
| `AWS_REGION`                            | `us-east-1` (SES Inbound NO existe en sa-east-1) |

---

## Pasos AWS para que recibir funcione

1. Verificar dominio `eduessence.com` en SES (DKIM + SPF en Route53).
2. Salir del **sandbox** SES (para poder enviar a cualquier destinatario).
3. Crear bucket `eduessence-inbox-raw-dev`.
4. Crear topic SNS `eduessence-inbound-emails-dev`.
5. Crear cola SQS `eduessence-inbound-emails-dev` + DLQ `eduessence-inbound-emails-dlq-dev`.
6. Suscribir la cola al topic.
7. En SES → **Email receiving** → crear receipt rule **catch-all** para `eduessence.com`:
   - Acción 1: **S3** → bucket `inbox-raw` + prefijo `ses-inbound/`
   - Acción 2: **SNS** → topic creado arriba
8. Apuntar MX records de `eduessence.com` a `inbound-smtp.us-east-1.amazonaws.com` (prio 10).
9. En `.env` poner `INBOX_SQS_ENABLED=true` + `INBOX_SQS_QUEUE_URL` + buckets.
10. El IAM role de la app debe tener permisos: `s3:GetObject`/`PutObject` sobre los 2 buckets, `sqs:Receive/Delete`, `ses:SendRawEmail`.

---

## Cómo se usa desde el front

```ts
// El admin crea un buzón nuevo
this.http.post('/api/buzones', {
  direccion: 'curso-podologia-2026@eduessence.com',
  nombreMostrar: 'Curso Podología 2026',
  tipo: 'CURSO',
  referenciaId: 42,
  forwardExternos: ['juan.instructor@gmail.com'],
  firma: '<b>Juan</b> · Instructor'
});

// Listar mis buzones (donde tengo acceso o soy admin)
this.http.get('/api/buzones/mios');

// Ver bandeja
this.http.get(`/api/mensajes/buzon/${buzonId}?carpeta=INBOX&page=0&size=20`);

// Responder
this.http.post(`/api/mensajes/${msgId}/responder`, {
  cuerpoHtml: '<p>Gracias por escribir...</p>',
  responderATodos: false
});
```

---

## Ejecución local

```bash
cd dev-ms-inbox
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Requiere: Eureka, MySQL con BD `eduessence_inbox`, credenciales AWS configuradas. En local poné `INBOX_SQS_ENABLED=false` y prueba el flujo de entrada con `POST /internal/sns/inbound` (cuerpo: payload SES de ejemplo).
