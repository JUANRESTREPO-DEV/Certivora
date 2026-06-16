package com.eduessence.cursos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI eduessenceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Eduessence — Cursos Service")
                .description("Catálogo de cursos, matrículas, progreso, exámenes y conectividad.")
                .version("1.0.0"));
    }
}
