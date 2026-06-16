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

### Con AWS CLI (desde la raíz `dev-ms-sendmail/src/main/resources`)

```bash
# Sube los 8 templates al bucket que tengas en BUCKET_TEMPLATE
aws s3 sync templates/ s3://eduessence-templates-dev/templates/ \
  --exclude "README.md" \
  --content-type "text/html; charset=utf-8" \
  --cache-control "public, max-age=300"
```

### Con la consola AWS

1. S3 → bucket `eduessence-templates-{env}` → carpeta `templates/`
2. Drag-and-drop los 8 `.html` (excluí el `README.md`)
3. En las propiedades de cada archivo, setear:
   - `Content-Type`: `text/html; charset=utf-8`
   - `Cache-Control`: `public, max-age=300`

### Para cada perfil

Cambia el bucket destino según el env activo:

| Env  | Bucket                                |
|------|---------------------------------------|
| dev  | `eduessence-templates-dev`            |
| qa   | `eduessence-templates-qa`             |
| prod | `eduessence-templates-prod`           |

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
