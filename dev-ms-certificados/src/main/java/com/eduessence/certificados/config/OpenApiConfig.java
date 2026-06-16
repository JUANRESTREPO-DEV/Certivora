package com.eduessence.certificados.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI eduessenceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Eduessence — Certificados Service")
                .description("Emisión, verificación pública y descarga de certificados (iText7 + QR + S3).")
                .version("1.0.0"));
    }
}
