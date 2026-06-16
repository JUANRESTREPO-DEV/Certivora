package com.eduessence.auth.service.impl;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.AccionDef;
import com.eduessence.auth.model.dto.EffectivePermissions;
import com.eduessence.auth.model.dto.ModuloDef;
import com.eduessence.auth.model.dto.SubmoduloDef;
import com.eduessence.auth.model.entity.*;
import com.eduessence.auth.model.enums.AccionEstado;
import com.eduessence.auth.model.enums.OverrideTipo;
import com.eduessence.auth.repository.*;
import com.eduessence.auth.service.PermissionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cálculo de permisos efectivos y gestión de overrides.
 *
 * Fórmula:
 *   efectivos = (∪ rol_permiso para roles del usuario)
 *             ∪ {p ∈ usuario_permiso : tipo = ALLOW}
 *             \ {p ∈ usuario_permiso : tipo = DENY}
 *
 * El rol ADMIN concede todos los permisos automáticamente (cortocircuito).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionsServiceImpl implements PermissionsService {

    private static final String ROL_ADMIN = "ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final PermisoRepository permisoRepository;
    private final ModuloRepository moduloRepository;
    private final SubmoduloRepository submoduloRepository;
    private final AccionRepository accionRepository;

    /* ───────────────────────── Lecturas ───────────────────────── */

    @Override
    @Transactional(readOnly = true)
    public List<ModuloDef> sidebar(Long usuarioId) {
        EffectivePermissions eff = efectivosDe(usuarioId);
        Set<String> permisos = eff.getPermisos();
        boolean admin = eff.isAdmin();

        Map<Long, ModuloDef> porModulo = new LinkedHashMap<>();
        for (Submodulo sub : submoduloRepository.findAllByActivoTrueOrderByModulo_OrdenAscOrdenAsc()) {
            String permisoEntrada = sub.getPermisoEntrada() == null ? null : sub.getPermisoEntrada().getCodigo();
            if (!admin && permisoEntrada != null && !permisos.contains(permisoEntrada)) {
                continue;
            }
            Modulo m = sub.getModulo();
            if (Boolean.FALSE.equals(m.getActivo())) continue;

            ModuloDef modDef = porModulo.computeIfAbsent(m.getId(), id -> ModuloDef.builder()
                    .id(m.getId()).codigo(m.getCodigo()).label(m.getLabel())
                    .icon(m.getIcon()).orden(m.getOrden())
                    .submodulos(new ArrayList<>())
                    .build());
            modDef.getSubmodulos().add(SubmoduloDef.builder()
                    .id(sub.getId()).codigo(sub.getCodigo()).label(sub.getLabel())
                    .path(sub.getPath()).icon(sub.getIcon()).orden(sub.getOrden())
                    .permisoEntrada(permisoEntrada)
                    .build());
        }
        return new ArrayList<>(porModulo.values());
    }

    @Override
    @Transactional(readOnly = true)
    public EffectivePermissions efectivosDe(Long usuarioId) {
        Usuario usuario = findUsuario(usuarioId);
        return computar(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccionDef> accionesDe(Long usuarioId) {
        EffectivePermissions eff = efectivosDe(usuarioId);
        Set<String> permisos = eff.getPermisos();
        boolean admin = eff.isAdmin();

        List<AccionDef> out = new ArrayList<>();
        for (Accion a : accionRepository.findAllActivasConPermisos()) {
            List<String> requeridos = a.getPermisos().stream()
                    .filter(ap -> Boolean.TRUE.equals(ap.getRequired()))
                    .map(ap -> ap.getPermiso().getCodigo())
                    .toList();
            List<String> opcionales = a.getPermisos().stream()
                    .filter(ap -> Boolean.FALSE.equals(ap.getRequired()))
                    .map(ap -> ap.getPermiso().getCodigo())
                    .toList();
            List<String> concedidos = new ArrayList<>(requeridos);
            concedidos.addAll(opcionales);
            concedidos.retainAll(permisos);

            AccionEstado estado = admin ? AccionEstado.GRANTED : calcularEstado(requeridos, permisos);

            out.add(AccionDef.builder()
                    .id(a.getId()).codigo(a.getCodigo()).label(a.getLabel())
                    .descripcion(a.getDescripcion()).icon(a.getIcon()).tipo(a.getTipo())
                    .orden(a.getOrden())
                    .submoduloId(a.getSubmodulo().getId())
                    .submoduloCodigo(a.getSubmodulo().getCodigo())
                    .estado(estado)
                    .permisosRequeridos(requeridos)
                    .permisosOpcionales(opcionales)
                    .permisosConcedidos(concedidos)
                    .build());
        }
        return out;
    }

    /* ───────────────────────── Mutaciones ───────────────────────── */

    @Override
    @Transactional
    public EffectivePermissions aplicarOverride(Long usuarioId, String codigoPermiso, String tipoStr, Long adminId) {
        OverrideTipo tipo = parseOverride(tipoStr);
        Usuario usuario = findUsuario(usuarioId);
        Permiso permiso = findPermiso(codigoPermiso);

        UsuarioPermiso existente = usuarioPermisoRepository
                .findByUsuario_IdAndPermiso_Codigo(usuarioId, codigoPermiso)
                .orElse(null);

        if (existente != null) {
            existente.setTipo(tipo);
            existente.setAsignadoPorUsuarioId(adminId);
            usuarioPermisoRepository.save(existente);
        } else {
            usuarioPermisoRepository.save(UsuarioPermiso.builder()
                    .usuario(usuario).permiso(permiso).tipo(tipo)
                    .asignadoPorUsuarioId(adminId)
                    .build());
        }
        return computar(usuario);
    }

    @Override
    @Transactional
    public EffectivePermissions quitarOverride(Long usuarioId, String codigoPermiso) {
        Usuario usuario = findUsuario(usuarioId);
        usuarioPermisoRepository
                .findByUsuario_IdAndPermiso_Codigo(usuarioId, codigoPermiso)
                .ifPresent(usuarioPermisoRepository::delete);
        return computar(usuario);
    }

    @Override
    @Transactional
    public EffectivePermissions aplicarBundle(Long usuarioId, Long idAccion, String tipoStr, Long adminId) {
        boolean grant = parseBundle(tipoStr);
        Usuario usuario = findUsuario(usuarioId);
        Accion accion = accionRepository.findById(idAccion)
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Acción no existe"));

        Set<Permiso> requeridos = accion.getPermisos().stream()
                .filter(ap -> Boolean.TRUE.equals(ap.getRequired()))
                .map(AccionPermiso::getPermiso)
                .collect(Collectors.toSet());

        OverrideTipo tipo = grant ? OverrideTipo.ALLOW : OverrideTipo.DENY;
        for (Permiso p : requeridos) {
            UsuarioPermiso up = usuarioPermisoRepository
                    .findByUsuario_IdAndPermiso_Codigo(usuarioId, p.getCodigo())
                    .orElse(null);
            if (up != null) {
                up.setTipo(tipo);
                up.setAsignadoPorUsuarioId(adminId);
                usuarioPermisoRepository.save(up);
            } else {
                usuarioPermisoRepository.save(UsuarioPermiso.builder()
                        .usuario(usuario).permiso(p).tipo(tipo)
                        .asignadoPorUsuarioId(adminId).build());
            }
        }
        return computar(usuario);
    }

    @Override
    @Transactional
    public EffectivePermissions resetearARol(Long usuarioId) {
        Usuario usuario = findUsuario(usuarioId);
        usuarioPermisoRepository.deleteByUsuario_Id(usuarioId);
        return computar(usuario);
    }

    @Override
    @Transactional
    public EffectivePermissions copiarDesde(Long idDestino, Long idOrigen, Long adminId) {
        if (Objects.equals(idDestino, idOrigen)) {
            throw new AuthApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Usuario origen y destino son iguales");
        }
        Usuario destino = findUsuario(idDestino);
        findUsuario(idOrigen);

        List<UsuarioPermiso> origen = usuarioPermisoRepository.findAllByUsuario_Id(idOrigen);
        usuarioPermisoRepository.deleteByUsuario_Id(idDestino);
        for (UsuarioPermiso up : origen) {
            usuarioPermisoRepository.save(UsuarioPermiso.builder()
                    .usuario(destino).permiso(up.getPermiso()).tipo(up.getTipo())
                    .asignadoPorUsuarioId(adminId).build());
        }
        return computar(destino);
    }

    /* ───────────────────────── Helpers ───────────────────────── */

    private EffectivePermissions computar(Usuario usuario) {
        Set<String> roles = usuario.getRoles().stream().map(Rol::getCodigo).collect(Collectors.toSet());
        boolean admin = roles.contains(ROL_ADMIN);

        Set<String> base = usuario.getRoles().stream()
                .flatMap(r -> r.getPermisos().stream())
                .map(Permiso::getCodigo)
                .collect(Collectors.toCollection(TreeSet::new));

        List<UsuarioPermiso> overrides = usuarioPermisoRepository.findAllByUsuario_Id(usuario.getId());
        Set<String> allows = overrides.stream()
                .filter(o -> o.getTipo() == OverrideTipo.ALLOW)
                .map(o -> o.getPermiso().getCodigo())
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> denies = overrides.stream()
                .filter(o -> o.getTipo() == OverrideTipo.DENY)
                .map(o -> o.getPermiso().getCodigo())
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> efectivos = new TreeSet<>(base);
        efectivos.addAll(allows);
        efectivos.removeAll(denies);

        return EffectivePermissions.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .roles(roles)
                .permisosBase(base)
                .overridesAllow(allows)
                .overridesDeny(denies)
                .permisos(efectivos)
                .admin(admin)
                .build();
    }

    private AccionEstado calcularEstado(List<String> requeridos, Set<String> permisos) {
        if (requeridos.isEmpty()) return AccionEstado.GRANTED;
        long hits = requeridos.stream().filter(permisos::contains).count();
        if (hits == 0) return AccionEstado.DENIED;
        if (hits == requeridos.size()) return AccionEstado.GRANTED;
        return AccionEstado.PARTIAL;
    }

    private Usuario findUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.USUARIO_NO_ENCONTRADO));
    }

    private Permiso findPermiso(String codigo) {
        return permisoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Permiso no existe: " + codigo));
    }

    private OverrideTipo parseOverride(String tipo) {
        try {
            return OverrideTipo.valueOf(tipo);
        } catch (Exception ex) {
            throw new AuthApiException(ServerApiStatusCode.DATOS_INVALIDOS, "tipo debe ser ALLOW o DENY");
        }
    }

    private boolean parseBundle(String tipo) {
        if ("GRANT".equalsIgnoreCase(tipo)) return true;
        if ("REVOKE".equalsIgnoreCase(tipo)) return false;
        throw new AuthApiException(ServerApiStatusCode.DATOS_INVALIDOS, "tipo debe ser GRANT o REVOKE");
    }
}
