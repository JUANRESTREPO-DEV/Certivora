# dev-ms-streaming

Transmisiones en vivo para cursos `VIRTUAL_LIVE`. Conecta OBS (o cualquier
otro encoder RTMP) con la audiencia vía HLS. Combinación tipo Udemy + YouTube.

## Arquitectura

```
┌──────────┐      RTMP push           ┌────────────────────┐    HLS    ┌──────────┐
│   OBS    │ ───────────────────────► │  Provider (AWS IVS │ ────────► │  Viewer  │
│ (instr.) │  rtmp://.../{streamKey}  │  Nginx-RTMP / Mux) │   .m3u8   │ (browser)│
└──────────┘                          └─────────┬──────────┘           └────┬─────┘
                                                │ webhook                   │ heartbeat
                                                ▼                           ▼
                                       ┌────────────────────────────────────────┐
                                       │            dev-ms-streaming             │
                                       │  - genera streamKey + ingestUrl + HLS   │
                                       │  - persiste sesión y eventos viewers    │
                                       │  - reenvía agregados a dev-ms-cursos    │
                                       └────────────────────────────────────────┘
```

## Cómo configurar OBS

1. El instructor crea la sesión: `POST /api/streams` → recibe
   ```json
   {
     "sessionId": 123,
     "provider": "AWS_IVS",
     "ingestUrl": "rtmps://xxx.global-contribute.live-video.net:443/app/",
     "streamKey": "sk_xxx",
     "playbackUrl": "https://cdn.eduessence.com/.../master.m3u8"
   }
   ```
2. En OBS Studio:
   - Menú **Settings → Stream**
   - **Service**: Custom
   - **Server**: pega `ingestUrl`
   - **Stream Key**: pega `streamKey`
   - **Start Streaming**
3. Cuando el provider detecta el publish hace callback al webhook
   `POST /internal/webhook/{provider}` → el MS cambia `estado=EN_VIVO`.

## Proveedores soportados (abstracción)

| Provider | Estado | Uso típico |
|---|---|---|
| `MOCK` | ✅ implementado | Dev local (URLs falsas, no ingiere video real) |
| `AWS_IVS` | 🟡 esqueleto | QA/prod managed (latencia ultra-baja) |
| `NGINX_RTMP` | ⏸ pendiente | Self-hosted (open source) |
| `MUX` | ⏸ pendiente | Managed alternativo |

Activación: `eduessence.streaming.provider=AWS_IVS` en application.properties.
Cada implementación es una clase `@ConditionalOnProperty` que provee el bean
`StreamProvider`.

## Endpoints

### Instructor
- `POST /api/streams` body `{ cursoId, sesionVirtualId?, titulo }` →
  `StreamCredentialsResponse`
- `POST /api/streams/{id}/terminar`
- `GET /api/streams/{id}`

### Viewer (player web)
- `GET /api/streams/{id}/playback` — URL HLS
- `POST /api/streams/{id}/viewer-event` body `{ evento, segundosAcumulados }` —
  el player envía cada 30 s (heartbeat)

### Internos
- `POST /internal/webhook/aws-ivs` — eventos de IVS (publish, stop)
- `POST /internal/webhook/nginx-rtmp/on-publish` — para `on_publish` de nginx-rtmp
- `GET /internal/streams/{id}` — para `dev-ms-cursos`

## Heartbeats y conectividad

El player envía `POST /api/streams/{id}/viewer-event` cada 30 segundos
con `evento=HEARTBEAT` y `segundosAcumulados`. Al terminar la sesión, el
MS calcula tiempo total conectado por viewer y envía vía Feign al
`dev-ms-cursos /internal/asistencia-virtual/heartbeat` para que actualice
`asistencia_virtual.porcentaje_presencia`.

Si la presencia < `presencia_minima_pct` del curso → no aprueba la sesión.

## Tablas

- `stream_session` — una por transmisión (curso, instructor, key, urls, estado, viewers_peak)
- `viewer_log` — eventos JOIN/HEARTBEAT/LEAVE/BUFFERING/ERROR por usuario+sesión

## Stack

- Java 21 · Spring Boot 3.4.5 · Spring Cloud 2024.0.0
- AWS SDK v2 (módulo `ivs`)
- MySQL 8 (RDS) + Flyway
- Variables del `.env` raíz: `SERVER_PORT_STREAMING`, `STREAMING_PROVIDER`,
  `DBDATABASE`, `RDS_USER`, `RDS_PASSWORD`, `AWS_REGION`, `EUREKA_URL`

## Estado actual

Listo: entidades (StreamSession, ViewerLog), enums, abstracción `StreamProvider`,
implementación `MockStreamProvider` activa por defecto, esqueleto
`AwsIvsStreamProvider`, repos, `StreamingService` con creación de sesiones,
marcado en-vivo/terminar, registro de eventos viewer y cálculo de peak,
config multi-perfil + Flyway V1.

**Próximo paso**: implementar el cliente real de AWS IVS (`createChannel` +
`createStreamKey`), webhook handlers por provider, agregador periódico de
heartbeats que reenvía a `dev-ms-cursos`.
