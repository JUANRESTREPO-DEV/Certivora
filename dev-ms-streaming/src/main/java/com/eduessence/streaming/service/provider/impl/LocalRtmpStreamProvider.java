package com.eduessence.streaming.service.provider.impl;

import com.eduessence.streaming.model.enums.ProviderTipo;
import com.eduessence.streaming.service.provider.StreamProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Provider para un <strong>servidor RTMP local</strong> (típicamente MediaMTX
 * o Nginx-RTMP corriendo en tu máquina). Devuelve URLs reales que apuntan
 * al servidor configurado, así OBS sí puede publicar y el browser sí puede
 * reproducir HLS.
 *
 * <h3>Cómo usar MediaMTX en Windows</h3>
 * <ol>
 *   <li>Descarga el .zip de <a href="https://github.com/bluenviron/mediamtx/releases">
 *       MediaMTX releases</a> (por ejemplo {@code mediamtx_v1.x.x_windows_amd64.zip})</li>
 *   <li>Descomprime en {@code C:\mediamtx\} y ejecuta {@code mediamtx.exe}</li>
 *   <li>Por defecto escucha en:
 *      <ul>
 *        <li>RTMP ingest: {@code rtmp://localhost:1935/<streamKey>}</li>
 *        <li>HLS playback: {@code http://localhost:8888/<streamKey>/index.m3u8}</li>
 *      </ul>
 *   </li>
 *   <li>Activa este provider:
 *      <pre>
 *        STREAMING_PROVIDER=LOCAL_RTMP
 *        LOCAL_RTMP_INGEST_HOST=192.168.2.4    # tu IP en la LAN
 *        LOCAL_RTMP_PLAYBACK_HOST=192.168.2.4  # misma IP
 *      </pre>
 *   </li>
 * </ol>
 *
 * <p>OBS debe usar la IP de la red local (no {@code localhost}) si está en
 * otro equipo. Si todo corre en la misma máquina, {@code localhost} sirve.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eduessence.streaming.provider", havingValue = "LOCAL_RTMP")
public class LocalRtmpStreamProvider implements StreamProvider {

    private final SecureRandom random = new SecureRandom();

    @Value("${eduessence.streaming.local.ingest-host:localhost}")
    private String ingestHost;

    @Value("${eduessence.streaming.local.ingest-port:1935}")
    private int ingestPort;

    @Value("${eduessence.streaming.local.ingest-path:}")
    private String ingestPath; // ej. "live" o vacío para usar la raíz

    @Value("${eduessence.streaming.local.playback-host:localhost}")
    private String playbackHost;

    @Value("${eduessence.streaming.local.playback-port:8888}")
    private int playbackPort;

    @Value("${eduessence.streaming.local.playback-scheme:http}")
    private String playbackScheme;

    @Override
    public ProviderTipo getTipo() {
        return ProviderTipo.LOCAL_RTMP;
    }

    @Override
    public StreamCredentials crear(String titulo) {
        // Stream key único — funciona como id del stream en MediaMTX
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        String streamKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(keyBytes)
                .replaceAll("[^A-Za-z0-9]", "")
                .substring(0, 20);

        String basePath = ingestPath == null || ingestPath.isBlank() ? "" : "/" + ingestPath;
        String ingestUrl = String.format("rtmp://%s:%d%s", ingestHost, ingestPort, basePath);

        // En MediaMTX el playback HLS se sirve en /<streamKey>/index.m3u8
        String playbackUrl = String.format("%s://%s:%d/%s/index.m3u8",
                playbackScheme, playbackHost, playbackPort, streamKey);

        String resourceId = UUID.randomUUID().toString();
        log.info("LOCAL_RTMP stream creado · resource={} · ingest={} · key={}",
                resourceId, ingestUrl, streamKey);

        return new StreamCredentials(resourceId, ingestUrl, streamKey, playbackUrl);
    }

    @Override
    public void eliminar(String providerResourceId) {
        // MediaMTX no requiere "borrar" recursos por adelantado — solo deja de
        // aceptar el streamKey. Marcamos como no-op aquí.
        log.debug("LOCAL_RTMP eliminar resource={} (no-op)", providerResourceId);
    }
}
