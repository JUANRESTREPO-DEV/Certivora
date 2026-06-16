package com.eduessence.sendmail.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eduessenceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eduessence — Sendmail Service")
                        .description("Envío de correos con templates dinámicos almacenados en S3 y renderizados con Velocity.")
                        .version("1.0.0")
                        .contact(new Contact().name("Eduessence").email("dev@eduessence.com")));
    }
}
