package com.eduessence.pagos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI eduessenceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Eduessence — Pagos Service")
                .description("Pagos manuales (llave Bre-B) + cupones + reservas de cupo.")
                .version("1.0.0"));
    }
}
