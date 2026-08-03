package com.eduessence.auth.service.impl;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.feign.SendmailServiceClient;
import com.eduessence.auth.model.dto.ActualizarUsuarioRequest;
import com.eduessence.auth.model.dto.CrearUsuarioRequest;
import com.eduessence.auth.model.dto.RolResponse;
import com.eduessence.auth.model.dto.UsuarioDetalleResponse;
import com.eduessence.auth.model.dto.UsuarioDetalleResponse.RolDto;
import com.eduessence.auth.model.dto.UsuarioListResponse;
import com.eduessence.auth.model.entity.AuditLog;
import com.eduessence.auth.model.entity.PasswordResetToken;
import com.eduessence.auth.model.entity.Persona;
import com.eduessence.auth.model.entity.Rol;
import com.eduessence.auth.model.entity.Usuario;
import com.eduessence.auth.model.enums.EstadoUsuario;
import com.eduessence.auth.repository.AuditLogRepository;
import com.eduessence.auth.repository.PasswordResetTokenRepository;
import com.eduessence.auth.repository.RolRepository;
import com.eduessence.auth.repository.UsuarioRepository;
import com.eduessence.auth.service.UsuariosAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuariosAdminServiceImpl implements UsuariosAdminService {

    private static final int RESET_TOKEN_MINUTOS = 30;
    /** Los tokens de "set-password" para altas nuevas viven más — el usuario
     * puede tardar en revisar el correo. 7 días es un punto razonable. */
    private static final int SET_PASSWORD_TOKEN_MINUTOS = 60 * 24 * 7;
    private static final String ROL_DEFAULT = "ASISTENTE";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SendmailServiceClient sendmail;

    @Value("${app.frontend.url}")
    private String appFrontendUrl;

    private final SecureRandom random = new SecureRandom();

    /* ─────────────────── Listar / obtener ─────────────────── */

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioListResponse> listar(String q, String estado, String rol, Pageable pageable) {
        String filtro = q == null ? "" : q.trim().toLowerCase();
        EstadoUsuario est = null;
        if (estado != null && !estado.isBlank()) {
            try { est = EstadoUsuario.valueOf(estado.trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        String rolCodigo = rol == null || rol.isBlank() ? null : rol.trim().toUpperCase();

        // Paginación en memoria — el volumen esperado es pequeño (miles).
        List<Usuario> all = usuarioRepository.findAll();
        final EstadoUsuario estFinal = est;
        List<UsuarioListResponse> filtrados = all.stream()
                .filter(u -> estFinal == null || u.getEstado() == estFinal)
                .filter(u -> rolCodigo == null || u.getRoles().stream()
                        .anyMatch(r -> rolCodigo.equals(r.getCodigo())))
                .filter(u -> filtro.isEmpty() || matches(u, filtro))
                .sorted(Comparator.comparing(this::sortKey, String.CASE_INSENSITIVE_ORDER))
                .map(UsuariosAdminServiceImpl::toList)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtrados.size());
        List<UsuarioListResponse> pageContent = start >= filtrados.size()
                ? List.of() : filtrados.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtrados.size());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDetalleResponse obtener(Long id) {
        return toDetalle(findOrThrow(id));
    }

    /* ─────────────────── Crear ─────────────────── */

    @Override
    @Transactional
    public UsuarioDetalleResponse crear(CrearUsuarioRequest req, Long actorId) {
        String email = req.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthApiException(ServerApiStatusCode.EMAIL_DUPLICADO);
        }

        // Username auto-generado a partir de nombres + apellidos.
        String nombres = req.getNombres().trim();
        String apellidos = req.getApellidos().trim();
        String username = generarUsername(nombres, apellidos);

        // Password inicial: random largo — el usuario nunca la conoce, la
        // define él mismo al hacer clic en el enlace de "establecer contraseña".
        String passwordPlaceholder = generarPasswordAleatoria();

        Set<Rol> roles = resolverRoles(req.getRoles());
        if (roles.isEmpty()) {
            Rol rolDefault = rolRepository.findByCodigo(ROL_DEFAULT)
                    .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.ROL_NO_ENCONTRADO));
            roles = Set.of(rolDefault);
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(passwordPlaceholder))
                .estado(EstadoUsuario.ACTIVO)
                .emailVerificado(false)
                .intentosFallidos(0)
                .roles(roles)
                .build();

        Persona persona = Persona.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .tipoDocumento(trim(req.getTipoDocumento()))
                .documento(trim(req.getDocumento()))
                .telefono(trim(req.getTelefono()))
                .pais(trim(req.getPais()))
                .ciudad(trim(req.getCiudad()))
                .profesion(trim(req.getProfesion()))
                .institucion(trim(req.getInstitucion()))
                .usuario(usuario)
                .build();
        usuario.setPersona(persona);

        usuarioRepository.save(usuario);
        audit(actorId, "USUARIO_CREADO", "OK",
                "id=" + usuario.getId() + " email=" + email + " username=" + username);

        // Correo de bienvenida (informativo — le da contexto de la plataforma).
        enviarBienvenida(usuario);
        // Correo con el link de "establecer contraseña" (accionable — 7 días).
        enviarSetPassword(usuario);

        return toDetalle(usuario);
    }

    /* ─────────────────── Actualizar ─────────────────── */

    @Override
    @Transactional
    public UsuarioDetalleResponse actualizar(Long id, ActualizarUsuarioRequest req, Long actorId) {
        Usuario u = findOrThrow(id);

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String nuevo = req.getEmail().trim().toLowerCase();
            if (!nuevo.equalsIgnoreCase(u.getEmail())
                    && usuarioRepository.existsByEmailIgnoreCase(nuevo)) {
                throw new AuthApiException(ServerApiStatusCode.EMAIL_DUPLICADO);
            }
            u.setEmail(nuevo);
        }
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            String nuevo = req.getUsername().trim().toLowerCase();
            if (!nuevo.equalsIgnoreCase(u.getUsername())
                    && usuarioRepository.existsByUsernameIgnoreCase(nuevo)) {
                throw new AuthApiException(ServerApiStatusCode.USERNAME_DUPLICADO);
            }
            u.setUsername(nuevo);
        }
        if (req.getEstado() != null && !req.getEstado().isBlank()) {
            try {
                u.setEstado(EstadoUsuario.valueOf(req.getEstado().trim().toUpperCase()));
                if (u.getEstado() != EstadoUsuario.BLOQUEADO) u.setBloqueadoHasta(null);
            } catch (IllegalArgumentException ignored) {}
        }
        if (req.getRoles() != null) {
            u.setRoles(resolverRoles(req.getRoles()));
        }

        Persona p = u.getPersona();
        if (p == null) {
            p = Persona.builder().usuario(u).build();
            u.setPersona(p);
        }
        if (req.getNombres() != null) p.setNombres(req.getNombres().trim());
        if (req.getApellidos() != null) p.setApellidos(req.getApellidos().trim());
        if (req.getTipoDocumento() != null) p.setTipoDocumento(trim(req.getTipoDocumento()));
        if (req.getDocumento() != null) p.setDocumento(trim(req.getDocumento()));
        if (req.getTelefono() != null) p.setTelefono(trim(req.getTelefono()));
        if (req.getPais() != null) p.setPais(trim(req.getPais()));
        if (req.getCiudad() != null) p.setCiudad(trim(req.getCiudad()));
        if (req.getProfesion() != null) p.setProfesion(trim(req.getProfesion()));
        if (req.getInstitucion() != null) p.setInstitucion(trim(req.getInstitucion()));

        usuarioRepository.save(u);
        audit(actorId, "USUARIO_EDITADO", "OK", "id=" + id);
        return toDetalle(u);
    }

    /* ─────────────────── Estado / roles / reset ─────────────────── */

    @Override
    @Transactional
    public UsuarioDetalleResponse cambiarEstado(Long id, String nuevoEstado, Long actorId) {
        Usuario u = findOrThrow(id);
        EstadoUsuario nuevo;
        try { nuevo = EstadoUsuario.valueOf(nuevoEstado.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) {
            throw new AuthApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Estado inválido");
        }
        u.setEstado(nuevo);
        if (nuevo != EstadoUsuario.BLOQUEADO) u.setBloqueadoHasta(null);
        usuarioRepository.save(u);
        audit(actorId, "USUARIO_ESTADO_" + nuevo.name(), "OK", "id=" + id);
        return toDetalle(u);
    }

    @Override
    @Transactional
    public UsuarioDetalleResponse actualizarRoles(Long id, List<String> codigosRoles, Long actorId) {
        Usuario u = findOrThrow(id);
        u.setRoles(resolverRoles(codigosRoles));
        usuarioRepository.save(u);
        audit(actorId, "USUARIO_ROLES_ACTUALIZADOS", "OK",
                "id=" + id + " roles=" + codigosRoles);
        return toDetalle(u);
    }

    @Override
    @Transactional
    public void solicitarResetPassword(Long id, Long actorId) {
        Usuario u = findOrThrow(id);
        String token = generarToken();
        PasswordResetToken prt = PasswordResetToken.builder()
                .usuario(u)
                .token(token)
                .expira(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTOS))
                .usado(false)
                .build();
        resetTokenRepository.save(prt);

        try {
            String nombre = u.getPersona() != null ? u.getPersona().getNombres() : u.getUsername();
            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", "RESET_PASSWORD",
                    "destinatario", Map.of(
                            "correo", u.getEmail(),
                            "nombre", nombre
                    ),
                    "variables", Map.of(
                            "token", token,
                            "minutos", RESET_TOKEN_MINUTOS,
                            "urlReset", appFrontendUrl + "/reset-password?token=" + token
                    )
            ));
        } catch (Exception ex) {
            log.warn("No pudimos enviar reset a {}: {}", u.getEmail(), ex.getMessage());
        }

        audit(actorId, "RESET_PASSWORD_FORZADO", "OK",
                "id=" + id + " email=" + u.getEmail());
    }

    /* ─────────────────── Roles ─────────────────── */

    @Override
    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        // Conteo por rol (memoria; N usuarios × N roles baja)
        Map<Long, Long> countPorRol = new HashMap<>();
        for (Usuario u : usuarioRepository.findAll()) {
            for (Rol r : u.getRoles()) {
                countPorRol.merge(r.getId(), 1L, Long::sum);
            }
        }
        return rolRepository.findAll().stream()
                .sorted(Comparator.comparing(Rol::getCodigo))
                .map(r -> RolResponse.builder()
                        .id(r.getId())
                        .codigo(r.getCodigo())
                        .nombre(r.getNombre())
                        .totalUsuarios(countPorRol.getOrDefault(r.getId(), 0L))
                        .build())
                .toList();
    }

    /* ─────────────────── Helpers ─────────────────── */

    private Usuario findOrThrow(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.USUARIO_NO_ENCONTRADO));
    }

    private Set<Rol> resolverRoles(List<String> codigos) {
        if (codigos == null) return new HashSet<>();
        Set<Rol> out = new HashSet<>();
        for (String c : codigos) {
            if (c == null || c.isBlank()) continue;
            rolRepository.findByCodigo(c.trim().toUpperCase()).ifPresent(out::add);
        }
        return out;
    }

    private static boolean matches(Usuario u, String q) {
        if (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) return true;
        if (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)) return true;
        Persona p = u.getPersona();
        if (p == null) return false;
        if (p.getNombres() != null && p.getNombres().toLowerCase().contains(q)) return true;
        if (p.getApellidos() != null && p.getApellidos().toLowerCase().contains(q)) return true;
        if (p.getDocumento() != null && p.getDocumento().toLowerCase().contains(q)) return true;
        String full = (p.getNombres() == null ? "" : p.getNombres()) + " " +
                (p.getApellidos() == null ? "" : p.getApellidos());
        return full.toLowerCase().contains(q);
    }

    private String sortKey(Usuario u) {
        Persona p = u.getPersona();
        return p == null || p.getNombres() == null
                ? (u.getUsername() == null ? "" : u.getUsername())
                : (p.getNombres() + " " + (p.getApellidos() == null ? "" : p.getApellidos()));
    }

    private String generarToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Contraseña aleatoria fuerte — nunca la ve nadie. Actúa como placeholder
     *  hasta que el usuario establezca la suya vía el enlace del email. */
    private String generarPasswordAleatoria() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Genera un username único a partir de nombres + apellidos: primera(s)
     *  letra(s) del nombre + apellido en lowercase, con sufijo numérico si
     *  ya existe. Espejo del helper en {@code AuthServiceImpl}. */
    private String generarUsername(String nombres, String apellidos) {
        String n = nombres == null ? "" : nombres.trim();
        String a = apellidos == null ? "" : apellidos.trim();
        String base = (n.substring(0, Math.min(2, n.length())) + a.replaceAll("\\s+", ""))
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) base = "usuario";
        String candidato = base;
        int sufijo = 1;
        while (usuarioRepository.existsByUsernameIgnoreCase(candidato)) {
            candidato = base + sufijo++;
            if (sufijo > 9999) break;
        }
        return candidato;
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }

    /**
     * Envía el correo de bienvenida a un usuario recién creado por un admin.
     * Es informativo — le presenta la plataforma y menciona que en un correo
     * aparte le llega el enlace para configurar su contraseña.
     */
    private void enviarBienvenida(Usuario u) {
        try {
            String nombres = u.getPersona() != null ? u.getPersona().getNombres() : u.getUsername();
            Map<String, Object> vars = new HashMap<>();
            vars.put("nombreDestinatario", nombres);
            vars.put("urlLogin", appFrontendUrl + "/login");

            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", "WELCOME",
                    "destinatario", Map.of(
                            "correo", u.getEmail(),
                            "nombre", nombres
                    ),
                    "variables", vars
            ));
        } catch (Exception ex) {
            log.warn("No pudimos enviar bienvenida a {}: {}", u.getEmail(), ex.getMessage());
        }
    }

    /**
     * Envía el correo con el enlace para establecer la contraseña. Crea un
     * {@link PasswordResetToken} con expiración larga (7 días) y usa el
     * template {@code RESET_PASSWORD}; el usuario define su clave por primera
     * vez desde {@code /set-password?token=…}.
     */
    private void enviarSetPassword(Usuario u) {
        String token = generarToken();
        PasswordResetToken prt = PasswordResetToken.builder()
                .usuario(u)
                .token(token)
                .expira(LocalDateTime.now().plusMinutes(SET_PASSWORD_TOKEN_MINUTOS))
                .usado(false)
                .build();
        resetTokenRepository.save(prt);

        try {
            String nombres = u.getPersona() != null ? u.getPersona().getNombres() : u.getUsername();
            Map<String, Object> vars = new HashMap<>();
            vars.put("token", token);
            vars.put("minutos", SET_PASSWORD_TOKEN_MINUTOS);
            // Admin-creado → ruta de bienvenida /set-password (contexto: primera vez).
            // Distinta del self-service /reset-password que usa AuthServiceImpl.recuperarPassword.
            vars.put("urlReset", appFrontendUrl + "/set-password?token=" + token);
            vars.put("username", u.getUsername());
            vars.put("email", u.getEmail());
            vars.put("nombreDestinatario", nombres);

            sendmail.enviarEmail(Map.of(
                    "nombreTemplate", "RESET_PASSWORD",
                    "destinatario", Map.of(
                            "correo", u.getEmail(),
                            "nombre", nombres
                    ),
                    "variables", vars
            ));
        } catch (Exception ex) {
            log.warn("No pudimos enviar set-password a {}: {}", u.getEmail(), ex.getMessage());
        }
    }

    private void audit(Long actorId, String accion, String resultado, String detalle) {
        try {
            AuditLog log = AuditLog.builder()
                    .usuarioId(actorId)
                    .accion(accion)
                    .resultado(resultado)
                    .detalle(detalle)
                    .fecha(LocalDateTime.now())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception ex) {
            UsuariosAdminServiceImpl.log.warn("No se pudo auditar {}: {}", accion, ex.getMessage());
        }
    }

    /* ─────────────────── Mappers ─────────────────── */

    private static UsuarioListResponse toList(Usuario u) {
        Persona p = u.getPersona();
        return UsuarioListResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .nombres(p == null ? null : p.getNombres())
                .apellidos(p == null ? null : p.getApellidos())
                .telefono(p == null ? null : p.getTelefono())
                .documento(p == null ? null : p.getDocumento())
                .avatarUrl(u.getAvatarUrl())
                .estado(u.getEstado() == null ? null : u.getEstado().name())
                .emailVerificado(u.getEmailVerificado())
                .ultimoAcceso(u.getUltimoAcceso())
                .fechaCreacion(u.getFechaCreacion())
                .roles(u.getRoles().stream()
                        .map(Rol::getCodigo)
                        .sorted()
                        .collect(Collectors.toList()))
                .build();
    }

    private static UsuarioDetalleResponse toDetalle(Usuario u) {
        Persona p = u.getPersona();
        List<RolDto> roles = u.getRoles().stream()
                .sorted(Comparator.comparing(Rol::getCodigo))
                .map(r -> RolDto.builder()
                        .id(r.getId()).codigo(r.getCodigo()).nombre(r.getNombre())
                        .build())
                .collect(Collectors.toList());
        return UsuarioDetalleResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .estado(u.getEstado() == null ? null : u.getEstado().name())
                .emailVerificado(u.getEmailVerificado())
                .nombres(p == null ? null : p.getNombres())
                .apellidos(p == null ? null : p.getApellidos())
                .tipoDocumento(p == null ? null : p.getTipoDocumento())
                .documento(p == null ? null : p.getDocumento())
                .telefono(p == null ? null : p.getTelefono())
                .pais(p == null ? null : p.getPais())
                .ciudad(p == null ? null : p.getCiudad())
                .profesion(p == null ? null : p.getProfesion())
                .institucion(p == null ? null : p.getInstitucion())
                .ultimoAcceso(u.getUltimoAcceso())
                .fechaCreacion(u.getFechaCreacion())
                .fechaActualizacion(u.getFechaActualizacion())
                .bloqueadoHasta(u.getBloqueadoHasta())
                .roles(roles)
                .build();
    }
}
