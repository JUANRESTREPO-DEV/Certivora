package com.eduessence.inbox.service;

import com.eduessence.inbox.model.dto.request.ComposerRequest;
import com.eduessence.inbox.model.dto.request.ResponderRequest;
import com.eduessence.inbox.model.dto.response.MensajeDetalleResponse;
import com.eduessence.inbox.model.dto.response.MensajeResponse;
import com.eduessence.inbox.model.enums.Carpeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MensajeService {

    Page<MensajeResponse> listarDeBuzon(Long buzonId, Carpeta carpeta, Long usuarioActual, Pageable pageable);

    Page<MensajeResponse> buscar(Long buzonId, String q, Long usuarioActual, Pageable pageable);

    MensajeDetalleResponse obtener(Long mensajeId, Long usuarioActual);

    void marcarLeido(Long mensajeId, boolean leido, Long usuarioActual);

    void mover(Long mensajeId, Carpeta carpeta, Long usuarioActual);

    void destacar(Long mensajeId, boolean destacado, Long usuarioActual);

    /** Responde a un mensaje existente: arma headers In-Reply-To + References. */
    Long responder(Long mensajeId, ResponderRequest req, Long usuarioActual);

    /** Compone un correo nuevo desde un buzón. */
    Long componer(ComposerRequest req, Long usuarioActual);
}
