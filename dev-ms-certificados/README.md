# dev-ms-certificados

Microservicio de **emisión y verificación de certificados digitales** para Eduessence.

Responsabilidades:

- Generar el PDF del diploma a partir de un *template* parametrizable en S3 (posiciones JSON).
- Adjuntar un **código QR** que apunta a la URL pública de verificación.
- Almacenar el PDF resultante en S3 (privado) y exponer **URLs pre-firmadas** de 24h.
- Llevar un registro inmutable de cada certificado con su **hash SHA-256** para verificación pública.
- Notificar al alumno vía `dev-ms-sendmail` cuando se emita su certificado.

---

## Stack

- Spring Boot 3.4.5 · Java 21 · Lombok
- MySQL 8 (AWS RDS) · Flyway
- AWS S3 (SDK v2) — `S3Client` + `S3Presigner`
- iText7 8.0.5 — generación PDF
- ZXing 3.5.3 — generación QR
- OpenFeign — `sendmail-service`
- Eureka client + SpringDoc OpenAPI

---

## Puerto y contexto

```
SERVER_PORT_CERTIFICADOS=8088
SERVER_SERVLET_CONTEXT_PATH_CERTIFICADOS=/certificados
```

Accesible vía API Gateway en `https://<gw>/certificados/**`.

---

## Esquema de BD (Flyway V1)

| Tabla                 | Notas                                                                      |
|-----------------------|----------------------------------------------------------------------------|
| `diploma_template`    | Template por `curso_id` + `tipo_participante`. `posiciones` es JSON.       |
| `certificado_emitido` | Único por `matricula_id`. Contiene `codigo`, `hash_verificacion`, `url_pdf`.|

---

## Flujo de emisión

```
dev-ms-cursos                dev-ms-certificados                  S3                sendmail
    │  matricula APROBADA          │                              │                    │
    │ ──POST /internal/emitir────▶ │                              │                    │
    │                              │  buscar DiplomaTemplate      │                    │
    │                              │  generar codigo EDU-YYYY-N   │                    │
    │                              │  generar hash SHA-256        │                    │
    │                              │  generar QR ZXing            │                    │
    │                              │  render PDF iText7           │                    │
    │                              │ ──putObject(certificados/)─▶│                    │
    │                              │  save certificado_emitido    │                    │
    │                              │ ────────────────────────────────────POST /enviar─▶│
    │ ◀───CertificadoResponse───── │                              │                    │
```

**Idempotencia:** si ya existe un `certificado_emitido` para esa `matricula_id`, se devuelve el existente sin volver a generar nada.

---

## Endpoints

| Método | Path                                          | Descripción                                              |
|--------|-----------------------------------------------|----------------------------------------------------------|
| GET    | `/api/certificados/mis`                       | Lista certificados del usuario autenticado.             |
| GET    | `/api/certificados/{codigo}/descargar`        | Devuelve URL pre-firmada (24h) al PDF.                  |
| GET    | `/api/verificar/{codigo}`                     | **Público.** Devuelve datos del certificado + hash.     |
| POST   | `/internal/certificados/emitir`               | Llamado por `cursos-service` cuando aprueba un alumno.  |

---

## Layout en S3

```
s3://eduessence-templates-diploma-{env}/
   cursos/{cursoId}/{tipoParticipante}/template.pdf

s3://eduessence-certificados-{env}/
   certificados/{codigo}.pdf
```

El bucket de **certificados** debe ser privado: el acceso se entrega siempre vía *presigned URL*.

---

## Variables de entorno

Se toman del `.env` raíz de `Sistema_Eventos_Dev`.

| Variable                                  | Ejemplo dev                                  |
|-------------------------------------------|----------------------------------------------|
| `SERVER_PORT_CERTIFICADOS`                | `8088`                                       |
| `SERVER_SERVLET_CONTEXT_PATH_CERTIFICADOS`| `/certificados`                              |
| `DBDATABASE`                              | `jdbc:mysql://...:3306/eduessence_certificados` |
| `RDS_USER` / `RDS_PASSWORD`               | credenciales de RDS                          |
| `BUCKET_CERTIFICADOS`                     | `eduessence-certificados-dev`                |
| `BUCKET_TEMPLATES_DIPLOMA`                | `eduessence-templates-diploma-dev`           |
| `CERT_URL_VERIFICACION_BASE`              | `https://eduessence.com/certificados/verificar/` |
| `CERT_HASH_SECRET`                        | (secret HMAC-SHA256)                         |
| `AWS_REGION`                              | `us-east-1`                                  |

En **prod** las credenciales DB se inyectan desde **AWS Secrets Manager** vía `DataSourceConfig` (bloque comentado, descomentar en deploy).

---

## Verificación pública

`GET /certificados/api/verificar/{codigo}` devuelve:

```json
{
  "codigo": "EDU-2025-000123",
  "valido": true,
  "alumno": "Juan Restrepo",
  "curso":  "Curso de Podología Summit 2025",
  "fechaEmision": "2025-11-30",
  "hash": "9af1...e2",
  "mensaje": "Certificado válido"
}
```

Cuando el certificado está **revocado** devuelve `valido = false` con motivo.

---

## Códigos de error

Prefijo `CER-*` — ver `ServerApiStatusCode`:

- `CER-404-1` Template no existe
- `CER-404-2` Certificado no existe
- `CER-409-1` Ya emitido (idempotencia interna)
- `CER-500-1` Error generando PDF
- `CER-500-2` Error subiendo a S3

---

## Ejecución local

```bash
cd dev-ms-certificados
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Requiere: `dev-ms-eureka` arriba en `:8761`, MySQL con DB `eduessence_certificados`, credenciales AWS configuradas (perfil local `~/.aws/credentials`).
