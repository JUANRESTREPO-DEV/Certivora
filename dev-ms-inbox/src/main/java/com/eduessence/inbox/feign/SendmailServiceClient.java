package com.eduessence.inbox.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "sendmail-service", path = "/sendmail",
        fallback = SendmailServiceFallback.class)
public interface SendmailServiceClient {

    @PostMapping("/internal/enviar")
    Map<String, Object> enviarEmail(@RequestBody Map<String, Object> payload);
}
