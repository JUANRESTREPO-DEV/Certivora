package com.eduessence.inbox.service.impl;

import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import com.eduessence.inbox.service.S3StorageService;
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

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${eduessence.aws.s3.bucket-inbox-raw}")
    private String bucketRaw;

    @Value("${eduessence.aws.s3.bucket-inbox-adjuntos}")
    private String bucketAdjuntos;

    @Override
    public byte[] descargarRaw(String key) {
        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(
                GetObjectRequest.builder().bucket(bucketRaw).key(key).build())) {
            return in.readAllBytes();
        } catch (Exception ex) {
            log.error("Error descargando .eml {}: {}", key, ex.getMessage());
            throw new InboxApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }

    @Override
    public String subirAdjunto(String key, byte[] bytes, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketAdjuntos)
                            .key(key)
                            .contentType(contentType == null ? "application/octet-stream" : contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
            return "s3://" + bucketAdjuntos + "/" + key;
        } catch (Exception ex) {
            log.error("Error subiendo adjunto {}: {}", key, ex.getMessage());
            throw new InboxApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }

    @Override
    public String urlPreFirmadaAdjunto(String key, long ttlSegundos) {
        try {
            GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(ttlSegundos))
                    .getObjectRequest(b -> b.bucket(bucketAdjuntos).key(key))
                    .build();
            return presigner.presignGetObject(req).url().toString();
        } catch (Exception ex) {
            throw new InboxApiException(ServerApiStatusCode.ERROR_S3, ex);
        }
    }
}
