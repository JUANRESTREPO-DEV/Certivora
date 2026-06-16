package com.eduessence.streaming.controller;

import com.eduessence.streaming.exception.ServerApiStatusCode;
import com.eduessence.streaming.exception.StreamingApiException;
import com.eduessence.streaming.model.dto.GeneralResponseDTO;
import com.eduessence.streaming.repository.StreamSessionRepository;
import com.eduessence.streaming.service.StreamingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhooks que los providers nos llaman para notificar publish/stop.
 *  - AWS IVS: configurar EventBridge → Lambda → POST aquí
 *  - Nginx-RTMP: configurar {@code on_publish}/{@code on_publish_done} callbacks
 */
@Slf4j
@Tag(name = "Webhooks")
@RestController
@RequestMapping("/internal/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final String SERVICE = "streaming-service";
    private final StreamingService streamingService;
    private final StreamSessionRepository sessionRepository;

    @PostMapping("/aws-ivs")
    public ResponseEntity<GeneralResponseDTO<Void>> awsIvs(@RequestBody Map<String, Object> payload) {
        log.info("Webhook AWS IVS: {}", payload);
        // Payload IVS: { "channelArn": "...", "stream_event_name": "Stream Start"|"Stream End" }
        String evento = String.valueOf(payload.get("stream_event_name"));
        String channelArn = String.valueOf(payload.get("channelArn"));
        sessionRepository.findAll().stream()
                .filter(s -> channelArn.equals(s.getProviderResourceId()))
                .findFirst()
                .ifPresent(s -> {
                    if ("Stream Start".equalsIgnoreCase(evento)) {
                        streamingService.marcarEnVivo(s.getId());
                    } else if ("Stream End".equalsIgnoreCase(evento)) {
                        streamingService.terminar(s.getId());
                    }
                });
        return ok();
    }

    @PostMapping("/nginx-rtmp/on-publish")
    public ResponseEntity<GeneralResponseDTO<Void>> nginxOnPublish(
            @RequestParam(value = "name", required = false) String streamKey
    ) {
        if (streamKey == null || streamKey.isBlank()) {
            throw new StreamingApiException(ServerApiStatusCode.WEBHOOK_INVALIDO);
        }
        sessionRepository.findByStreamKey(streamKey).ifPresent(s ->
                streamingService.marcarEnVivo(s.getId()));
        return ok();
    }

    @PostMapping("/nginx-rtmp/on-publish-done")
    public ResponseEntity<GeneralResponseDTO<Void>> nginxOnPublishDone(
            @RequestParam(value = "name", required = false) String streamKey
    ) {
        if (streamKey != null && !streamKey.isBlank()) {
            sessionRepository.findByStreamKey(streamKey)
                    .ifPresent(s -> streamingService.terminar(s.getId()));
        }
        return ok();
    }

    private static ResponseEntity<GeneralResponseDTO<Void>> ok() {
        return ResponseEntity.ok(GeneralResponseDTO.<Void>builder()
                .statusCode(200).serviceName(SERVICE).message("OK").build());
    }
}
