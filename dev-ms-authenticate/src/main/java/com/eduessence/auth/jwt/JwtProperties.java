package com.eduessence.auth.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "eduessence.jwt")
public class JwtProperties {
    private String secret;
    private long expirationMs = 3_600_000L;       // 1 h
    private long refreshExpirationMs = 604_800_000L; // 7 d
    private String issuer = "eduessence";
}
