package com.eduessence.auth.service;

import com.eduessence.auth.model.dto.AccionDef;
import com.eduessence.auth.model.dto.EffectivePermissions;
import com.eduessence.auth.model.dto.ModuloDef;

import java.util.List;

public interface PermissionsService {

    /** Sidebar visible para el usuario (módulos + submódulos cuyo permisoEntrada el usuario posea). */
    List<ModuloDef> sidebar(Long usuarioId);

    /** Permisos efectivos del usuario (con roles, overrides y flag admin). */
    EffectivePermissions efectivosDe(Long usuarioId);

    /** Catálogo de acciones con su estado calculado para el usuario. */
    List<AccionDef> accionesDe(Long usuarioId);

    /** Override individual ALLOW / DENY sobre un permiso para un usuario. */
    EffectivePermissions aplicarOverride(Long usuarioId, String codigoPermiso, String tipo, Long adminId);

    /** Borra el override individual (vuelve a heredar del rol). */
    EffectivePermissions quitarOverride(Long usuarioId, String codigoPermiso);

    /** Aplica/revoca atómicamente todos los permisos requeridos de una acción. */
    EffectivePermissions aplicarBundle(Long usuarioId, Long idAccion, String tipo, Long adminId);

    /** Elimina todos los overrides del usuario (vuelve al rol). */
    EffectivePermissions resetearARol(Long usuarioId);

    /** Copia los overrides del usuario origen al destino (sobrescribe los actuales). */
    EffectivePermissions copiarDesde(Long idDestino, Long idOrigen, Long adminId);
}
