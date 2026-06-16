package com.eduessence.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración CORS global del gateway.
 *
 *  - Se aplica un {@link CorsWebFilter} con orden {@code -200} para que se
 *    ejecute ANTES del {@link com.eduessence.apigateway.filter.JwtValidationFilter}
 *    (que tiene orden {@code -100}).
 *  - El JwtValidationFilter, además, deja pasar todos los OPTIONS para que
 *    el preflight pueda completarse correctamente.
 */
@Configuration
public class CorsConfig {

    @Value("${eduessence.cors.allowed-origins:http://localhost:4200,http://localhost:4201,https://0t69dtw7-4200.use.devtunnels.ms/}")
    private String[] allowedOrigins;

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Request-Id",
                "X-Status-Code",
                "Content-Disposition"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCorsConfiguration());
        return source;
    }

    /**
     * Filtro CORS reactivo con orden -200. Se ejecuta ANTES del JWT filter
     * (orden -100) para que los preflight OPTIONS reciban los headers
     * {@code Access-Control-Allow-*} sin tener que pasar la validación de token.
     */
    @Bean
    @Order(-200)
    public CorsWebFilter corsWebFilter(CorsConfigurationSource source) {
        return new CorsWebFilter(source);
    }
}
