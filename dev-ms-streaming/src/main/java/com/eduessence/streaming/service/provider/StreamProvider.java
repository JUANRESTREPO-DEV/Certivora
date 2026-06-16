package com.eduessence.streaming.service.provider;

import com.eduessence.streaming.model.enums.ProviderTipo;

/**
 * Abstracción del proveedor de streaming. Cada implementación encapsula
 * la API específica (AWS IVS / Nginx-RTMP / Mux / Mock) y devuelve los
 * datos comunes que necesita el resto del servicio.
 *
 * Para integrar OBS: el usuario configura
 *   Server      = {@code result.ingestUrl}
 *   Stream Key  = {@code result.streamKey}
 */
public interface StreamProvider {

    ProviderTipo getTipo();

    /** Crea el canal/recurso en el provider y devuelve credenciales OBS + URL HLS. */
    StreamCredentials crear(String titulo);

    /** Libera el recurso en el provider (canal IVS, etc.). */
    void eliminar(String providerResourceId);

    /** Marca el inicio (puede ser no-op si el provider lo detecta solo). */
    default void iniciar(String providerResourceId) {}

    /** Marca el fin y opcionalmente devuelve URL de grabación si está disponible. */
    default String terminar(String providerResourceId) { return null; }

    record StreamCredentials(
            String providerResourceId,
            String ingestUrl,     // ej: rtmps://ingest.eduessence.com:443/live
            String streamKey,     // único; el provider lo valida en publish
            String playbackUrl    // ej: https://cdn.eduessence.com/.../master.m3u8
    ) {}
}
