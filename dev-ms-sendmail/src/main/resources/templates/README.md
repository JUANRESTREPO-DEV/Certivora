# Templates de email — Eduessence

8 plantillas HTML para `dev-ms-sendmail`, alineadas con el seed V2 (`email_template`).

| Archivo                        | `nombre` (BD)            | Variables requeridas (R) / opcionales (O)                     |
|--------------------------------|--------------------------|---------------------------------------------------------------|
| `welcome.html`                 | `WELCOME`                | R: `nombreDestinatario` · O: `urlLogin`                       |
| `reset_password.html`          | `RESET_PASSWORD`         | R: `token` · O: `minutos`                                     |
| `curso_inscripcion_ok.html`    | `CURSO_INSCRIPCION_OK`   | R: `nombreCurso`, `fechaInicio` · O: `urlCurso`               |
| `pago_llave_pendiente.html`    | `PAGO_LLAVE_PENDIENTE`   | R: `nombreCurso`, `llave`, `monto`, `horasReserva`            |
| `pago_aprobado.html`           | `PAGO_APROBADO`          | R: `nombreCurso`, `numeroReferencia`, `fechaInicio`           |
| `pago_rechazado.html`          | `PAGO_RECHAZADO`         | R: `nombreCurso` · O: `motivo`                                |
| `certificado_emitido.html`     | `CERTIFICADO_EMITIDO`    | R: `nombreCurso`, `codigoCertificado`, `urlVerificacion`, `urlPdf` |
| `summit_recordatorio.html`     | `SUMMIT_RECORDATORIO`    | R: `nombreEvento`, `fechaInicio` · O: `urlStreaming`          |

Todas reciben automáticamente las variables del sistema vía `EmailServiceImpl.construirContexto()`:
`nombreDestinatario`, `destinatarioCorreo`, `fecha`, `fechaActual`, `horaActual`, `anioActual`,
`empresaNombre`, `empresaCorreo`, `empresaSitioWeb`, `empresaTelefono`, `empresaLogoUrl`.

## Sintaxis

Apache Velocity 2.4.1:
- Variable: `${nombreVariable}`
- Condicional: `#if($urlStreaming && $urlStreaming != "") ... #else ... #end` (usado en `summit_recordatorio.html`)
- Loop: `#foreach($item in $coleccion) ... #end`

## Diseño

- Layout 100% table-based para compatibilidad con Outlook, Gmail, Apple Mail, Yahoo.
- Inline styles (los `<style>` no se respetan en muchos clientes).
- Paleta Eduessence: `#004aad` primario, `#ecad43` amarillo, `#d13633` rojo, `#15803d` verde para confirmaciones.
- Pre-header oculto en cada template (texto que se ve en el preview de la bandeja).
- Responsive a 600px max.
- Botones CTA con `border-radius: 999px` (pill).

## Subir a S3

Los templates viven en el bucket configurado por `BUCKET_TEMPLATE` (env var). El backend
los descarga vía `S3TemplateLoader.descargar(bucket, key)` donde `key` es el campo
`path_template` de la tabla `email_template` (ej: `templates/welcome.html`).

### Dev (local) — script helper

Dev no tiene CI/CD (todo corre en tu máquina), pero el sendmail local igual
lee los HTML desde S3. Un solo comando desde cualquier PowerShell:

```powershell
pwsh C:\Desarrollo\Sistema_Eventos_Dev\infra\10-sync-templates-dev.ps1
```

Sincroniza esta carpeta → `s3://edu-templates-dev/templates/`. Idempotente
— solo sube archivos cambiados.

### QA y PROD — auto-sync en CI/CD

Los workflows en el repo `Certivora-dev-ms-sendmail` sincronizan
automáticamente en cada push a `qa` / `main`:

- `infra/deploy-qa-workflow-template.yml` → `edu-templates-qa`
- `infra/deploy-prod-workflow-template.yml` → `edu-templates-prod`

### Manual con AWS CLI (fallback / hotfix)

Desde la raíz `dev-ms-sendmail/src/main/resources`:

```bash
aws s3 sync templates/ s3://edu-templates-dev/templates/ \
  --exclude "README.md" \
  --content-type "text/html; charset=utf-8" \
  --cache-control "public, max-age=300"
```

### Con la consola AWS

1. S3 → bucket `edu-templates-{env}` → carpeta `templates/`
2. Drag-and-drop los `.html` (excluí el `README.md`)
3. En las propiedades de cada archivo, setear:
   - `Content-Type`: `text/html; charset=utf-8`
   - `Cache-Control`: `public, max-age=300`

### Buckets por perfil

| Env  | Bucket                       |
|------|------------------------------|
| dev  | `edu-templates-dev`          |
| qa   | `edu-templates-qa`           |
| prod | `edu-templates-prod`         |

## Cómo probar un template localmente

Con el MS levantado:

```bash
curl -X POST http://localhost:8080/sendmail/internal/enviar \
  -H "Content-Type: application/json" \
  -d '{
    "nombreTemplate": "WELCOME",
    "destinatario": { "correo": "tucorreo@gmail.com", "nombre": "Juan" },
    "variables": {
      "urlLogin": "https://eduessence.com/login"
    }
  }'
```

(El endpoint `/internal/**` está en la lista `public-paths` del gateway, así
que no requiere JWT — pero solo es accesible desde la red interna.)

## Cambios visuales

Si querés ajustar el diseño:
- **Colores**: buscar `#004aad`, `#ecad43`, `#d13633` y reemplazar.
- **Logo**: descomentar la línea de `addInline()` en `EmailServiceImpl.enviarMime()`
  y agregar `<img src="cid:${logoContentId}" />` en el header del template.
- **Fuente**: cambiar `'Segoe UI',Arial,sans-serif` en cada `<body>`.
