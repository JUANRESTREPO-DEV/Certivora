package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.PresignedUrlRequest;
import com.eduessence.cursos.model.dto.response.PresignedUrlResponse;
import com.eduessence.cursos.service.S3PresignedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class S3PresignedServiceImpl implements S3PresignedService {

    private static final int UPLOAD_TTL_SEG = 600; // 10 minutos para subir
    private static final int READ_TTL_SEG = 3600;  // 1 hora para leer

    /**
     * Tamaño máx por archivo en MB. Configurable vía
     * {@code eduessence.upload.max-mb} (default 5120 MB = 5 GB).
     * Usa 0 o un valor negativo para deshabilitar el límite.
     */
    @Value("${eduessence.upload.max-mb:5120}")
    private long maxMb;

    private long maxBytes() {
        return maxMb <= 0 ? Long.MAX_VALUE : maxMb * 1024L * 1024L;
    }

    private final S3Presigner presigner;
    private final S3Client s3;

    @Value("${eduessence.aws.s3.bucket-cursos:edu-resources-dev}")
    private String bucket;

    @Value("${eduessence.aws.s3.cdn-base-url:}")
    private String cdnBaseUrl;

    @Value("${eduessence.api.public-base-url:http://localhost:8080/cursos}")
    private String publicBaseUrl;

    public S3PresignedServiceImpl(S3Presigner presigner, S3Client s3) {
        this.presigner = presigner;
        this.s3 = s3;
    }

    @Override
    public PresignedUrlResponse generarUploadUrl(Long cursoId, PresignedUrlRequest req) {
        if (req.getTamanoBytes() != null && req.getTamanoBytes() > maxBytes()) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El archivo excede el tamaño máximo permitido (" + maxMb + " MB)");
        }

        String ext = inferirExtension(req.getNombreArchivo());
        String key = String.format(Locale.ROOT, "cursos/%d/%s/%s%s",
                cursoId, req.getCarpeta(), UUID.randomUUID(), ext);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(req.getMime())
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(UPLOAD_TTL_SEG))
                .putObjectRequest(put)
                .build();

        PresignedPutObjectRequest signed = presigner.presignPutObject(presignReq);

        String urlPublica = cdnBaseUrl != null && !cdnBaseUrl.isBlank()
                ? cdnBaseUrl.replaceAll("/$", "") + "/" + key
                : signed.url().toString().split("\\?")[0];

        return PresignedUrlResponse.builder()
                .uploadUrl(signed.url().toString())
                .s3Key(key)
                .urlPublica(urlPublica)
                .expiraEnSegundos(UPLOAD_TTL_SEG)
                .build();
    }

    @Override
    public String generarReadUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return null;

        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(s3Key).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(READ_TTL_SEG))
                .getObjectRequest(get)
                .build();
        PresignedGetObjectRequest signed = presigner.presignGetObject(req);
        return signed.url().toString();
    }

    @Override
    public void borrar(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
        } catch (Exception ex) {
            log.warn("No se pudo borrar el objeto s3://{}/{}: {}", bucket, s3Key, ex.getMessage());
        }
    }

    @Override
    public PresignedUrlResponse subirArchivo(Long cursoId, MultipartFile file, String carpeta) {
        if (file == null || file.isEmpty()) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Archivo vacío");
        }
        if (file.getSize() > maxBytes()) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El archivo excede el tamaño máximo permitido (" + maxMb + " MB)");
        }
        if (carpeta == null || !carpeta.matches("[a-z0-9-]+")) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Carpeta inválida");
        }

        long id = cursoId == null ? 0L : cursoId;
        String ext = inferirExtension(file.getOriginalFilename());
        String key = String.format(Locale.ROOT, "cursos/%d/%s/%s%s",
                id, carpeta, UUID.randomUUID(), ext);

        String mime = file.getContentType();
        if (mime == null || mime.isBlank()) mime = "application/octet-stream";

        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(mime)
                    .contentLength(file.getSize())
                    .build();
            s3.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException ex) {
            log.error("Error leyendo bytes del MultipartFile: {}", ex.getMessage(), ex);
            throw new CursosApiException(ServerApiStatusCode.ERROR_INTERNO,
                    "No pudimos procesar el archivo. Inténtalo nuevamente.");
        } catch (Exception ex) {
            log.error("Error subiendo a S3 s3://{}/{}: {}", bucket, key, ex.getMessage(), ex);
            throw new CursosApiException(ServerApiStatusCode.ERROR_INTERNO,
                    "No pudimos guardar el archivo en S3.");
        }

        return PresignedUrlResponse.builder()
                .uploadUrl(null)               // no aplica — el archivo ya subió
                .s3Key(key)
                .urlPublica(buildPublicUrl(key))
                .expiraEnSegundos(0)
                .build();
    }

    /**
     * Construye la URL pública del archivo. Apunta al endpoint
     * {@code /api/public/uploads/stream} del backend (no a S3 directo),
     * para evitar problemas de CORS/ORB y exponer el bucket.
     *
     * <p><strong>Guardamos solo el path relativo</strong> (sin host) — el
     * frontend prepende el host actual al usarla. De este modo, si mañana
     * la URL del backend cambia (de IP local a dominio prod, etc.), los
     * registros guardados siguen funcionando sin migración.</p>
     *
     * <p>Si hay CDN configurado, ahí sí devolvemos URL absoluta porque el
     * CDN es estable y no cambia.</p>
     */
    private String buildPublicUrl(String key) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            return cdnBaseUrl.replaceAll("/$", "") + "/" + key;
        }
        // Path relativo desde el contexto del servicio (/cursos en gateway):
        // /api/public/uploads/stream?key=<key-url-encoded>
        return "/api/public/uploads/stream?key=" + java.net.URLEncoder.encode(key,
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String inferirExtension(String nombre) {
        if (nombre == null) return "";
        int dot = nombre.lastIndexOf('.');
        if (dot < 0 || dot == nombre.length() - 1) return "";
        String ext = nombre.substring(dot).toLowerCase(Locale.ROOT);
        // Sanitiza: sólo a-z 0-9 y punto
        return ext.replaceAll("[^a-z0-9.]", "");
    }
}
