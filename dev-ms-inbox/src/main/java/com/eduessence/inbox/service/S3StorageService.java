package com.eduessence.inbox.service;

public interface S3StorageService {
    /** Descarga el .eml crudo desde el bucket de SES Inbound. */
    byte[] descargarRaw(String key);

    /** Sube un adjunto extraído al bucket de adjuntos. */
    String subirAdjunto(String key, byte[] bytes, String contentType);

    /** Devuelve una URL pre-firmada de descarga (ttl en segundos). */
    String urlPreFirmadaAdjunto(String key, long ttlSegundos);
}
