package com.eduessence.auth.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SendmailServiceFallback implements SendmailServiceClient {

    @Override
    public void enviarEmail(Map<String, Object> payload) {
        log.warn("[Fallback] sendmail-service no disponible. Payload: {}", payload);
    }
}
