package com.eduessence.auth.service;

import com.eduessence.auth.model.dto.ActualizarPerfilRequest;
import com.eduessence.auth.model.dto.PerfilResponse;

public interface PerfilService {

    PerfilResponse obtener(Long usuarioId);

    PerfilResponse actualizar(Long usuarioId, ActualizarPerfilRequest request);

    PerfilResponse actualizarAvatar(Long usuarioId, String avatarUrl);
}
