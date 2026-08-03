package com.eduessence.certificados.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AuthenticateServiceFallback implements AuthenticateServiceClient {

    @Override
    public Map<String, Object> lookup(List<Long> ids) {
        log.warn("authenticate-service offline — lookup({}) devuelve vacío", ids);
        return Map.of("statusCode", 503, "response", List.of());
    }
}
