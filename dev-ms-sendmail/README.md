# dev-ms-sendmail

Envío de correos con **templates dinámicos**. Los templates HTML viven en
**S3**; el metadata y el contrato de variables viven en MySQL.

## Stack

- Java 21
- Spring Boot 3.4.5 + Spring Cloud 2024.0.0
- Apache Velocity 2.4 — render dinámico
- AWS SDK v2 — S3 client
- Spring Mail — SMTP
- MySQL 8 (RDS) + Flyway 10

## Flujo de envío dinámico

```
otro micro → POST /sendmail/internal/enviar-email
              {
                nombreTemplate: "WELCOME",
                asunto: "...",                     (opcional)
                destinatario: { correo, nombre },
                variables: { ... }
              }
              │
              ▼
1. busca template ACTIVO por nombre en email_template
2. descarga HTML desde S3 (path_template como key)
3. arma contexto Velocity = variables ∪ valores por defecto ∪ contexto sistema
4. valida que todas las variables `requerido=true` estén presentes
5. velocityEngine.evaluate(context, writer, "EmailTemplate", new StringReader(html))
6. JavaMailSender.send(MimeMessage)
7. inserta registro en email_envio (ENVIADO / FALLIDO + variables JSON)
```

## Endpoints

### Internos (sin auth — los llaman otros micros vía Feign)
| Método | Path | Descripción |
|---|---|---|
| `POST` | `/internal/enviar-email` | Envío dinámico con template |
| `POST` | `/internal/enviar-con-adjunto` | Multi-destinatario + adjuntos base64 (template opcional) |

### Admin (`/api/templates/**`)
| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/templates` | Listar templates activos |
| `GET` | `/api/templates/{id}` | Detalle |
| `POST` | `/api/templates` | Crear |
| `PUT` | `/api/templates/{id}` | Actualizar |
| `PUT` | `/api/templates/{id}/activar` `/desactivar` | Toggle estado |

## Variables del contexto Velocity

Cada render incluye **automáticamente**:

| Variable | Tipo | Origen |
|---|---|---|
| `destinatarioCorreo`, `destinatarioNombre`, `nombreDestinatario` | String | del request |
| `fecha`, `fechaActual`, `horaActual`, `anioActual` | String/Int | sistema (zona Bogotá) |
| `empresaNombre`, `empresaCorreo`, `empresaSitioWeb`, `empresaTelefono`, `empresaLogoUrl` | String | `application.yml` |
| `logoContentId` | String UUID | solo si `empresaLogoUrl` no está vacía |

Las variables específicas del template se declaran en `email_template_variable`
con `tipo_dato`, `requerido`, `valor_defecto`, `descripcion`. Si una variable
**requerida** falta en el request, el envío se rechaza con `SM-400-2`.

## Templates seedeados (V2)

| Nombre | Asunto default | Uso |
|---|---|---|
| `WELCOME` | Bienvenido a Eduessence | Tras registro |
| `RESET_PASSWORD` | Restablece tu contraseña | Recuperación |
| `CURSO_INSCRIPCION_OK` | Confirmación de inscripción a curso | Curso gratis o cupón 100% |
| `PAGO_LLAVE_PENDIENTE` | Tu llave de pago Bre-B | Pago iniciado, esperando confirmación |
| `PAGO_APROBADO` | Pago confirmado · ¡bienvenido al curso! | Admin aprobó pago |
| `PAGO_RECHAZADO` | No pudimos confirmar tu pago | Reserva liberada |
| `CERTIFICADO_EMITIDO` | Tu certificado está listo | Tras emisión |
| `SUMMIT_RECORDATORIO` | Tu Summit empieza pronto | Job 24h antes |

Los archivos HTML correspondientes deben subirse a S3 en
`templates/<nombre>.html` (lowercase) — el seed los referencia por esa key.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | |
| `SERVER_PORT` | `8087` | |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | — | RDS MySQL |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` | — | Servidor SMTP |
| `AWS_REGION` | `us-east-1` | |
| `S3_BUCKET_TEMPLATES` | — | Bucket de templates HTML |
| `EMPRESA_NOMBRE`, `EMPRESA_CORREO`, `EMPRESA_SITIO_WEB`, `EMPRESA_TELEFONO`, `EMPRESA_LOGO_URL` | defaults | Inyectadas en cada email |

## Cómo correr

```bash
cp .env.example .env   # completa SMTP + RDS + S3
./mvnw spring-boot:run
```

Swagger: http://localhost:8087/sendmail/swagger-ui.html

## Próximos pasos

1. Subir los HTML reales a S3 (`templates/welcome.html`, etc.).
2. Conectar Feign desde `dev-ms-authenticate` ya está listo —
   solo asegurarse de que el endpoint `/internal/enviar-email` esté en
   la lista de `public-paths` del gateway (ya lo está).
3. Implementar `setting-service` para que `empresaNombre`, `empresaLogoUrl`
   vengan de BD y no de `application.yml`.
