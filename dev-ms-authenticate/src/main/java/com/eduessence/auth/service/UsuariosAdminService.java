package com.eduessence.auth.service;

import com.eduessence.auth.model.dto.ActualizarUsuarioRequest;
import com.eduessence.auth.model.dto.CrearUsuarioRequest;
import com.eduessence.auth.model.dto.RolResponse;
import com.eduessence.auth.model.dto.UsuarioDetalleResponse;
import com.eduessence.auth.model.dto.UsuarioListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Gestión de usuarios para el back-office (/app/usuarios).
 * Todos los métodos son admin-only — se validan con {@code USUARIO_VER /
 * USUARIO_EDITAR / USUARIO_ROL} en el controller.
 */
public interface UsuariosAdminService {

    Page<UsuarioListResponse> listar(String q, String estado, String rol, Pageable pageable);

    UsuarioDetalleResponse obtener(Long id);

    UsuarioDetalleResponse crear(CrearUsuarioRequest req, Long actorId);

    UsuarioDetalleResponse actualizar(Long id, ActualizarUsuarioRequest req, Long actorId);

    UsuarioDetalleResponse cambiarEstado(Long id, String nuevoEstado, Long actorId);

    /** Reenvía un correo de recuperación con token — el mismo flujo que {@code recuperarPassword}. */
    void solicitarResetPassword(Long id, Long actorId);

    UsuarioDetalleResponse actualizarRoles(Long id, List<String> codigosRoles, Long actorId);

    List<RolResponse> listarRoles();
}
