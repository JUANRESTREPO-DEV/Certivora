# dev-ms-authenticate

Servicio de autenticación de Eduessence. Maneja usuarios, persona, roles, permisos y emisión de JWT.

## Stack

- Java 21
- Spring Boot 3.4.5
- Spring Cloud 2024.0.0 (Eureka Client, OpenFeign, Resilience4j)
- Spring Data JPA + Hibernate 6
- Spring Security 6 (BCrypt)
- JJWT 0.12.6
- MySQL 8 (RDS)
- Flyway 10

## Endpoints

### Públicos (`/auth/api/auth/**`)

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Login → JWT |
| `POST` | `/api/auth/register` | Crear cuenta |
| `POST` | `/api/auth/recuperar-password` | Solicitar reset por email |
| `POST` | `/api/auth/restablecer-password` | Aplicar nueva password con token |
| `POST` | `/api/auth/cerrar-sesion` | Logout (revoca JWT) |

### Privados (`/auth/api/perfil/**`)

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/perfil` | Mi perfil |
| `PUT` | `/api/perfil` | Actualizar mi perfil |

### Internos (`/auth/internal/**`)

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/internal/verificar-token` | Para otros micros |
| `GET` | `/internal/usuarios/{id}` | Datos de usuario por id |

### Permisos (`/auth/api/permissions/**`)

#### Self (usuario logueado)
| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/permissions/sidebar` | Módulos + submódulos visibles |
| `GET` | `/api/permissions/me` | Mis permisos efectivos |
| `GET` | `/api/permissions/actions/me` | Mis acciones con estado |

#### Admin (sobre otros usuarios)
| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/permissions/user/{id}/effective` | Efectivos de otro usuario |
| `GET` | `/api/permissions/actions/{id}` | Acciones de otro usuario |
| `PATCH` | `/api/permissions/user/{id}/permiso/{codigo}` | ALLOW / DENY individual |
| `DELETE` | `/api/permissions/user/{id}/permiso/{codigo}` | Quitar override |
| `PATCH` | `/api/permissions/user/{id}/action-bundle/{idAccion}` | GRANT / REVOKE bundle |
| `POST` | `/api/permissions/user/{id}/reset-to-role` | Volver al rol (quita overrides) |
| `POST` | `/api/permissions/user/{id}/copy-from/{idOrigen}` | Copiar overrides de otro |

## Cómo correr

```bash
# 1. Copia .env.example a .env y completa credenciales RDS + JWT_SECRET
cp .env.example .env

# 2. Asegúrate de que dev-ms-eureka esté arriba
# 3. Corre:
./mvnw spring-boot:run
```

Swagger: http://localhost:8082/auth/swagger-ui.html

## Base de datos

Flyway crea automáticamente las tablas y siembra roles + permisos al arrancar.

- `V1__init_schema.sql` — tablas usuario, persona, rol, permiso, password_reset_token, audit_log
- `V2__seed_roles_permisos.sql` — roles iniciales (ADMIN, INSTRUCTOR, ASISTENTE, INVITADO) y matriz de permisos
- `V3__permisos_jerarquicos.sql` — tablas modulo, submodulo, accion, accion_permiso, usuario_permiso
- `V4__seed_modulos_acciones.sql` — sidebar de Eduessence (Dashboard, Cursos, Pagos, Certificados, Eventos, Usuarios, Configuración) con sus acciones

## Modelo de permisos

```
Modulo (sidebar group)
  └─ Submodulo (pantalla con path Angular + permiso de entrada)
       └─ Accion (bundle: BUTTON/PAGE/SECTION)
            └─ AccionPermiso (N:N → Permiso atómico, con flag required)

Rol ─┬─ RolPermiso ───┬─→ Permiso atómico
     │                │
Usuario ─┬─ UsuarioRol┘
         │
         └─ UsuarioPermiso (override ALLOW/DENY a nivel usuario)
```

**Cálculo de efectivos**:
```
efectivos(usuario) = (∪ rol_permiso para roles del usuario)
                   ∪ {p ∈ usuario_permiso : tipo = ALLOW}
                   \ {p ∈ usuario_permiso : tipo = DENY}
```

**Estado de acción para un usuario**:
- `GRANTED` — el usuario tiene todos los permisos requeridos
- `PARTIAL` — el usuario tiene algunos
- `DENIED` — el usuario no tiene ninguno

El rol `ADMIN` corto-circuita y concede todo automáticamente.

## Seguridad

- JWT HS256, expiración 1 h
- BCrypt cost 12 para passwords
- 5 intentos fallidos → bloqueo 15 min
- Token de reset: 30 min, un solo uso
- Auditoría en `audit_log` para login, register, reset
