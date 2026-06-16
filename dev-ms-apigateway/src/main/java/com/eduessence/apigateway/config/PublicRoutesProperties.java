package com.eduessence.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrones Ant que NO requieren JWT.
 * Definidos en {@code eduessence.security.public-paths} de application.yml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "eduessence.security")
public class PublicRoutesProperties {

    private final PathMatcher matcher = new AntPathMatcher();
    private List<String> publicPaths = new ArrayList<>();

    public boolean matches(String path) {
        return publicPaths.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }
}
