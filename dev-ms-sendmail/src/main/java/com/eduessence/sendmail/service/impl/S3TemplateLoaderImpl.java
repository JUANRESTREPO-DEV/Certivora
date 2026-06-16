package com.eduessence.sendmail.service.impl;

import com.eduessence.sendmail.exception.SendmailApiException;
import com.eduessence.sendmail.exception.ServerApiStatusCode;
import com.eduessence.sendmail.service.S3TemplateLoader;
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
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3TemplateLoaderImpl implements S3TemplateLoader {

    private final S3Client s3;

    @Value("${eduessence.aws.s3.bucket-templates}")
    private String bucketTemplatesDefault;

    @Override
    public String descargar(String bucket, String key) {
        String effectiveBucket = (bucket == null || bucket.isBlank()) ? bucketTemplatesDefault : bucket;
        // Defensivo contra CRLF en .env
        effectiveBucket = effectiveBucket.trim();
        String effectiveKey = key == null ? null : key.trim();

        if (effectiveBucket == null || effectiveBucket.isBlank() || effectiveKey == null || effectiveKey.isBlank()) {
            throw new SendmailApiException(ServerApiStatusCode.ERROR_S3, "Bucket o key vacíos");
        }

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(effectiveBucket)
                .key(effectiveKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(req)) {
            byte[] bytes = in.readAllBytes();
            log.debug("Template descargado: {}/{} ({} bytes)", effectiveBucket, effectiveKey, bytes.length);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException | S3Exception ex) {
            log.error("Error descargando {}/{}: {}", effectiveBucket, effectiveKey, ex.getMessage());
            throw new SendmailApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }

    @Override
    public void subir(String bucket, String key, String content) {
        String effectiveBucket = (bucket == null || bucket.isBlank()) ? bucketTemplatesDefault : bucket;
        effectiveBucket = effectiveBucket.trim();
        String effectiveKey = key == null ? null : key.trim();

        if (effectiveBucket == null || effectiveBucket.isBlank() || effectiveKey == null || effectiveKey.isBlank()) {
            throw new SendmailApiException(ServerApiStatusCode.ERROR_S3, "Bucket o key vacíos");
        }

        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(effectiveBucket)
                .key(effectiveKey)
                .contentType("text/html; charset=utf-8")
                .build();

        try {
            s3.putObject(req, RequestBody.fromBytes(bytes));
            log.info("Template subido a S3: {}/{} ({} bytes)", effectiveBucket, effectiveKey, bytes.length);
        } catch (S3Exception ex) {
            log.error("Error subiendo {}/{}: {}", effectiveBucket, effectiveKey, ex.getMessage());
            throw new SendmailApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }
}
