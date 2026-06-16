package com.eduessence.certificados.service.impl;

import com.eduessence.certificados.exception.CertificadosApiException;
import com.eduessence.certificados.exception.ServerApiStatusCode;
import com.eduessence.certificados.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${eduessence.aws.s3.bucket-certificados}")
    private String bucketCertificados;

    @Value("${eduessence.aws.s3.bucket-templates-diploma}")
    private String bucketTemplates;

    /**
     * Base URL del MS de cursos para resolver template_url relativos del tipo
     * {@code /api/public/uploads/stream?key=...}. Cuando el admin sube el
     * fondo del diploma desde el form del curso, el archivo queda en el
     * bucket del MS-cursos y la URL pública apunta al streamer de ese MS.
     */
    @Value("${eduessence.cursos.public-base-url:http://localhost:8085}")
    private String cursosBaseUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String subirPdf(String key, byte[] bytes) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketCertificados).key(key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(bytes));
            return "s3://" + bucketCertificados + "/" + key;
        } catch (Exception ex) {
            log.error("Error subiendo PDF {}: {}", key, ex.getMessage(), ex);
            throw new CertificadosApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }

    @Override
    public String urlPreFirmada(String key, long ttlSegundos) {
        try {
            GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(ttlSegundos))
                    .getObjectRequest(b -> b.bucket(bucketCertificados).key(key))
                    .build();
            return presigner.presignGetObject(req).url().toString();
        } catch (Exception ex) {
            throw new CertificadosApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }

    @Override
    public byte[] descargarTemplate(String key) {
        if (key == null || key.isBlank()) {
            throw new CertificadosApiException(ServerApiStatusCode.ERROR_S3,
                    "templateUrl vacío");
        }
        String raw = key.trim();

        // El front guarda en diploma_template.template_url la urlPublica que
        // devuelve el MS-cursos: o absoluta (http://host/api/public/uploads/stream?key=...)
        // o relativa (/api/public/uploads/stream?key=...). Para esos dos
        // formatos hacemos HTTP GET — el archivo vive en el bucket del
        // MS-cursos, no en bucketTemplates de este MS.
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return descargarPorHttp(raw);
        }
        if (raw.startsWith("/")) {
            // Evitar duplicación si el path ya trae el context-path del
            // gateway (p. ej. cursosBaseUrl=http://host/cursos y
            // raw=/cursos/api/public/...).
            String base = cursosBaseUrl.replaceAll("/+$", "");
            String basePath;
            try {
                basePath = URI.create(base).getPath();
            } catch (Exception ex) {
                basePath = "";
            }
            String path = raw;
            if (basePath != null && !basePath.isEmpty() && !"/".equals(basePath)
                    && path.startsWith(basePath + "/")) {
                path = path.substring(basePath.length());
            }
            return descargarPorHttp(base + path);
        }

        // Fallback retrocompat: tratar como S3 key en bucketTemplates.
        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(
                GetObjectRequest.builder().bucket(bucketTemplates).key(raw).build())) {
            return in.readAllBytes();
        } catch (Exception ex) {
            log.error("Error descargando template {}: {}", raw, ex.getMessage());
            throw new CertificadosApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }

    private byte[] descargarPorHttp(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.error("HTTP {} descargando template {}", resp.statusCode(), url);
                throw new CertificadosApiException(ServerApiStatusCode.ERROR_S3,
                        "El servidor de archivos respondió " + resp.statusCode() + " para " + url);
            }
            return resp.body();
        } catch (CertificadosApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error descargando template {}: {}", url, ex.getMessage());
            throw new CertificadosApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }
}
