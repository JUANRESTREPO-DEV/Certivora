package com.eduessence.streaming.service;

import com.eduessence.streaming.model.dto.response.StreamCredentialsResponse;
import com.eduessence.streaming.model.dto.response.StreamSessionResponse;
import com.eduessence.streaming.model.enums.EventoViewer;

public interface StreamingService {

    /**
     * Crea una sesión nueva. Llama al provider configurado para generar
     * stream key + ingest URL + playback URL.
     */
    StreamCredentialsResponse crearSesion(Long cursoId, Long sesionVirtualId, Long instructorId, String titulo);

    /**
     * Regenera credenciales para una sesión existente. Útil cuando se cambia
     * el provider (ej. de MOCK a LOCAL_RTMP) o cuando la stream key se filtró.
     *
     * <p>Libera el recurso anterior en el provider y solicita uno nuevo. Sólo
     * permitido si el estado NO es {@code EN_VIVO} (no podemos cortar una
     * transmisión en curso).</p>
     */
    StreamCredentialsResponse regenerarCredenciales(Long sessionId);

    /** Cambia estado a EN_VIVO (típicamente llamado por webhook del provider). */
    StreamSessionResponse marcarEnVivo(Long sessionId);

    /** Termina la sesión, intenta obtener URL de grabación. */
    StreamSessionResponse terminar(Long sessionId);

    /**
     * Borra el recurso en el provider (canal IVS, etc.) y elimina la entidad.
     * Útil cuando se elimina la sesión virtual o se cambia su tipo a PRESENCIAL.
     */
    void eliminar(Long sessionId);

    StreamSessionResponse obtener(Long sessionId);

    StreamSessionResponse obtenerPorStreamKey(String streamKey);

    /**
     * Registra un evento del viewer (JOIN/HEARTBEAT/LEAVE). El heartbeat lo
     * envía el player web cada 30 s con {@code segundosAcumulados}.
     */
    void registrarEvento(Long sessionId, Long usuarioId, String sessionToken,
                         EventoViewer evento, Integer segundosAcumulados,
                         String ip, String userAgent);
}
