package com.eduessence.auth.service;

import com.eduessence.auth.model.dto.ActualizarPerfilRequest;
import com.eduessence.auth.model.dto.CambiarPasswordRequest;
import com.eduessence.auth.model.dto.PerfilResponse;

public interface PerfilService {

    PerfilResponse obtener(Long usuarioId);

    PerfilResponse actualizar(Long usuarioId, ActualizarPerfilRequest request);

    PerfilResponse actualizarAvatar(Long usuarioId, String avatarUrl);

    /**
     * Cambio de contraseña del propio usuario, conociendo la actual. Valida
     * la current con {@code PasswordEncoder.matches}. Emite audit
     * {@code PASSWORD_CAMBIADO}. Lanza {@code CREDENCIALES_INVALIDAS} si la
     * actual no coincide.
     */
    void cambiarPassword(Long usuarioId, CambiarPasswordRequest request);
}
