package com.eduessence.streaming.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ivs.IvsClient;

/**
 * Cliente AWS IVS. Solo se instancia cuando {@code eduessence.streaming.provider=AWS_IVS}
 * para no requerir credenciales AWS en dev local con provider MOCK.
 */
@Configuration
@ConditionalOnProperty(name = "eduessence.streaming.provider", havingValue = "AWS_IVS")
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public IvsClient ivsClient() {
        return IvsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
