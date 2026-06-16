package com.eduessence.inbox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI inboxOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Eduessence — Inbox Service API")
                .description("Bandeja corporativa con SES Inbound: buzones dinámicos, mensajes, alias, accesos, respuestas.")
                .version("v1")
                .contact(new Contact().name("Eduessence").email("dev@eduessence.com")));
    }
}
