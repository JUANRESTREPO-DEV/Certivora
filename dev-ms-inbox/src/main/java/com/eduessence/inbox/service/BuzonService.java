package com.eduessence.inbox.service;

import com.eduessence.inbox.model.dto.request.ActualizarBuzonRequest;
import com.eduessence.inbox.model.dto.request.CrearAliasRequest;
import com.eduessence.inbox.model.dto.request.CrearBuzonRequest;
import com.eduessence.inbox.model.dto.request.OtorgarAccesoRequest;
import com.eduessence.inbox.model.dto.response.BuzonResponse;
import com.eduessence.inbox.model.entity.Buzon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BuzonService {

    BuzonResponse crear(CrearBuzonRequest req, Long usuarioCreador);

    BuzonResponse obtener(Long id);

    BuzonResponse actualizar(Long id, ActualizarBuzonRequest req);

    void eliminar(Long id);

    Page<BuzonResponse> listar(Boolean soloActivos, Pageable pageable);

    List<BuzonResponse> misBuzones(Long usuarioId);

    BuzonResponse crearAlias(CrearAliasRequest req);

    void eliminarAlias(Long aliasId);

    BuzonResponse otorgarAcceso(Long buzonId, OtorgarAccesoRequest req, Long usuarioCreador);

    void revocarAcceso(Long buzonId, Long usuarioId);

    /** Para uso interno del listener SES Inbound: matchea por dirección/alias/catch-all. */
    Buzon resolverBuzonPorDireccion(String direccion);
}
