package com.eduessence.pagos.service.impl;

import com.eduessence.pagos.exception.PagosApiException;
import com.eduessence.pagos.exception.ServerApiStatusCode;
import com.eduessence.pagos.service.ComprobanteStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ComprobanteStorageServiceImpl implements ComprobanteStorageService {

    /** 25 MB — un comprobante (foto/PDF) jamás debería superarlo. */
    private static final long MAX_BYTES = 25L * 1024L * 1024L;

    /** Mime types aceptados. */
    private static final Set<String> MIMES_OK = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic",
            "application/pdf");

    private final S3Client s3;

    @Value("${eduessence.aws.s3.bucket-pagos:edu-pagos-dev}")
    private String bucket;

    @Value("${eduessence.api.public-base-url:http://localhost:8080/pagos}")
    private String publicBaseUrl;

    public ComprobanteStorageServiceImpl(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public SubidaResult subir(Long pagoId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Archivo vacío");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El archivo excede el máximo permitido (25 MB)");
        }
        String mime = file.getContentType();
        if (mime == null || !MIMES_OK.contains(mime.toLowerCase(Locale.ROOT))) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Formato no permitido. Acepta imágenes (JPG/PNG/WEBP/HEIC) o PDF.");
        }

        String ext = inferirExtension(file.getOriginalFilename(), mime);
        String key = String.format(Locale.ROOT, "pagos/%d/comprobante-%s%s",
                pagoId, UUID.randomUUID(), ext);

        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(mime)
                    .build();
            s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException ex) {
            throw new PagosApiException(ServerApiStatusCode.ERROR_INTERNO,
                    "Fallo subiendo el comprobante: " + ex.getMessage());
        }

        // Solo guardamos el path relativo (sin host). El front prepende el
        // host actual al usarla — si el backend cambia de IP/dominio, los
        // registros guardados siguen funcionando sin migración.
        String urlPublica = "/api/public/comprobantes/stream?key=" + key;

        return new SubidaResult(key, urlPublica, mime, file.getSize());
    }

    @Override
    public Descarga descargar(String s3Key) {
        if (s3Key == null || s3Key.isBlank() || s3Key.contains("..")) {
            throw new PagosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Key inválida");
        }
        GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(s3Key).build();
        ResponseBytes<GetObjectResponse> resp = s3.getObjectAsBytes(req);
        return new Descarga(resp.asByteArray(), resp.response().contentType());
    }

    private static String inferirExtension(String nombre, String mime) {
        if (nombre != null) {
            int dot = nombre.lastIndexOf('.');
            if (dot > 0 && dot < nombre.length() - 1) {
                return nombre.substring(dot).toLowerCase(Locale.ROOT);
            }
        }
        return switch (mime == null ? "" : mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
