package com.eduessence.cursos.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Propaga las cabeceras de identidad y trazabilidad inyectadas por el gateway
 * (X-User-Id, X-User-Roles, X-User-Email, etc.) hacia las llamadas Feign
 * salientes a otros microservicios.
 *
 * <p>Sin esto, cuando cursos-service llama a streaming-service vía Feign,
 * la request va sin esos headers y streaming-service responde
 * {@code 400 "X-User-Id ausente"}.
 */
@Slf4j
@Configuration
public class FeignAuthInterceptor {

    /** Headers del request entrante que queremos propagar tal cual a Feign. */
    private static final List<String> HEADERS_PROPAGAR = Arrays.asList(
            "x-user-id",
            "x-user-roles",
            "x-user-email",
            "x-user-name",
            "authorization",
            "x-request-id",
            "x-trace-id"
    );

    @Bean
    public RequestInterceptor feignHeaderPropagator() {
        return (RequestTemplate template) -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                // No hay request HTTP en curso (ej: schedulers internos) — nada que propagar
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            for (String header : HEADERS_PROPAGAR) {
                String value = request.getHeader(header);
                if (value != null && !value.isBlank()) {
                    template.header(header, value);
                }
            }
        };
    }
}
