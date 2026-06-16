package com.eduessence.cursos.service;

import com.eduessence.cursos.model.dto.request.PresignedUrlRequest;
import com.eduessence.cursos.model.dto.response.PresignedUrlResponse;
import org.springframework.web.multipart.MultipartFile;

public interface S3PresignedService {

    /**
     * Genera una URL pre-firmada para hacer PUT a S3. El front sube los bytes
     * directamente al bucket; el backend nunca toca el binario.
     * <p>Vigente como API alternativa; el front estándar usa
     * {@link #subirArchivo(Long, MultipartFile, String)} (multipart) para
     * evitar CORS en S3.
     */
    PresignedUrlResponse generarUploadUrl(Long cursoId, PresignedUrlRequest req);

    /**
     * Sube un archivo desde el backend a S3. El binario viaja por el backend
     * (multipart/form-data), evitando exponer credenciales al browser y
     * sin necesidad de configurar CORS en el bucket.
     *
     * @param cursoId curso al que pertenece (0 = uploads temporales pre-creación)
     * @param file    archivo recibido en el endpoint
     * @param carpeta subcarpeta lógica (presentacion-video, curso-logo, ...)
     * @return key destino + URL pública (mismo formato que la presigned)
     */
    PresignedUrlResponse subirArchivo(Long cursoId, MultipartFile file, String carpeta);

    /**
     * Genera una URL pre-firmada GET de corta duración para leer un objeto.
     * Útil para mostrar videos/imágenes en el front sin exponer el bucket.
     */
    String generarReadUrl(String s3Key);

    /** Borra un objeto del bucket. Best-effort: si falla, log y sigue. */
    void borrar(String s3Key);
}
