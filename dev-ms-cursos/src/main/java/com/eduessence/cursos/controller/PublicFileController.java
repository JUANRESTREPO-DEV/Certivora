package com.eduessence.cursos.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Endpoint público que sirve archivos almacenados en S3 a través del backend.
 *
 * <p>El binario va desde S3 → cursos-service → browser sin que el browser
 * acceda directamente a S3 (evita CORS/ORB y oculta el bucket).
 *
 * <p>No requiere JWT — el secreto es la key UUID generada al subir, que es
 * computacionalmente no adivinable. Para autorizar lectura por permiso o
 * usuario, hay que mover este endpoint fuera de {@code /api/public/...} y
 * añadir un guard.
 */
@Tag(name = "Archivos públicos")
@RestController
@RequestMapping("/api/public/uploads")
@RequiredArgsConstructor
@Slf4j
public class PublicFileController {

    private final S3Client s3;

    @Value("${eduessence.aws.s3.bucket-cursos:eduessence-cursos-dev}")
    private String bucket;

    /**
     * Stream del archivo identificado por {@code key} (path dentro del bucket).
     * El navegador llama a esta URL desde {@code <img src=...>} o GET fetch.
     */
    @GetMapping("/stream")
    public ResponseEntity<InputStreamResource> stream(@RequestParam("key") String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ResponseInputStream<GetObjectResponse> object = s3.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            GetObjectResponse meta = object.response();

            String mime = meta.contentType();
            MediaType mediaType = (mime == null || mime.isBlank())
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(mime);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            if (meta.contentLength() != null) {
                headers.setContentLength(meta.contentLength());
            }
            // Caché agresiva en cliente — los archivos son inmutables (key UUID)
            headers.setCacheControl("public, max-age=31536000, immutable");

            return new ResponseEntity<>(new InputStreamResource(object), headers, 200);
        } catch (NoSuchKeyException nsk) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error sirviendo archivo s3://{}/{}: {}", bucket, key, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
