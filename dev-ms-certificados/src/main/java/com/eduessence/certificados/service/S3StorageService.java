package com.eduessence.certificados.service;

public interface S3StorageService {
    /** Sube un PDF al bucket de certificados. Devuelve la URL S3. */
    String subirPdf(String key, byte[] bytes);

    /** Genera URL pre-firmada (válida por TTL) para descarga privada. */
    String urlPreFirmada(String key, long ttlSegundos);

    /** Descarga el template (imagen o PDF base) desde S3. */
    byte[] descargarTemplate(String key);
}
