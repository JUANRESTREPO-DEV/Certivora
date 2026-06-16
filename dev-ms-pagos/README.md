# dev-ms-pagos

Pagos manuales con llave Bre-B + cupones de descuento + reserva de cupo.

## Flujo

```
1. Usuario en /curso_info elige modalidad → POST /api/pagos/iniciar
   { cursoId, modalidades, cuponCodigo? }

2. Si trae cuponCodigo:
   - GET /api/cupones/validar (R-CUP-01 a 12)
   - calcula montoFinal = montoOriginal - descuento
   - si montoFinal == 0:
       pago.estado = GRATIS_POR_CUPON
       cupon.usos_actuales++
       cupon_uso(pago_id)
       → matricula ACTIVA inmediata (vía Feign a cursos)
       FIN

3. Si montoFinal > 0:
   - pago.estado = PENDIENTE_LLAVE
   - pago.llave = generador único (formato: EDU-YYYYMMDD-NNNN)
   - reserva_expira = now + 48h (parametrizable)
   - cupon.usos_actuales++ y cupon_uso registrado
   - retorna { llave, montoFinal, reservaExpira }

4. Usuario paga vía Bre-B usando la llave → marca POST /api/pagos/{id}/confirmar-pago
   con comprobante (URL S3) → estado pasa a PENDIENTE_CONFIRMACION

5. Admin revisa y POST /api/pagos/{id}/aprobar
   - pago.estado = APROBADO
   - dispara Feign a cursos → activa matrícula
   - dispara Feign a sendmail → envía PAGO_APROBADO

6. Job dev-ms-jobs: cada hora, busca pagos PENDIENTE_LLAVE con
   reserva_expira < now → estado=EXPIRADO + cupon.liberar()
```

## Endpoints planificados

### Cupones — usuario
- `GET /api/cupones/validar?codigo=X&cursoId=Y&monto=Z` — validar (no consume)

### Cupones — admin
- `POST /api/cupones` — crear
- `GET /api/cupones` — listar con filtros
- `PUT /api/cupones/{id}/activar` `/desactivar`
- `GET /api/cupones/{id}/usos` — auditoría

### Pagos
- `POST /api/pagos/iniciar` — genera llave o GRATIS_POR_CUPON
- `POST /api/pagos/{id}/confirmar-pago` — usuario marca como pagado
- `POST /api/pagos/{id}/aprobar` — admin confirma
- `POST /api/pagos/{id}/rechazar` — admin rechaza
- `GET /api/mis-pagos` — historial

### Internos
- `POST /internal/pagos/expirar-vencidos` — llamado por dev-ms-jobs
- `GET /internal/pagos/{id}` — para dev-ms-cursos

## Stack y configuración

- Java 21 · Spring Boot 3.4.5 · Spring Cloud 2024.0.0
- MySQL 8 (RDS) + Flyway
- Variables del `.env` raíz: `SERVER_PORT_PAGOS=8086`, `SERVER_SERVLET_CONTEXT_PATH_PAGOS=/pagos`, `DBDATABASE`, `RDS_USER`, `RDS_PASSWORD`, `WOMPI_*` (futuro)

## Estado actual

Listo: entidades (Cupon, CuponUso, Pago) + enums (EstadoPago, MetodoPago, TipoDescuento, AlcanceCupon) + repos + `CuponService` con las reglas R-CUP-01..12 + exception stack + properties multi-perfil + Flyway V1.

**Próximo paso**: `PagoService` (iniciar, aprobar, rechazar), controllers, Feign a cursos+sendmail, integración Wompi opcional.
