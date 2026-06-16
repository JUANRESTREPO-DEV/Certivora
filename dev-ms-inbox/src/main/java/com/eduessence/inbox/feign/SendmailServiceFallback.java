package com.eduessence.inbox.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SendmailServiceFallback implements SendmailServiceClient {
    @Override
    public Map<String, Object> enviarEmail(Map<String, Object> payload) {
        log.warn("Sendmail Feign down — payload descartado: {}", payload.keySet());
        return Map.of("statusCode", 503, "message", "sendmail-service no disponible");
    }
}
