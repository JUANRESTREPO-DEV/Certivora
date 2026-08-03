package com.eduessence.sendmail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${eduessence.aws.region}")
    private String region;

    /**
     * Reusable provider — {@code DefaultCredentialsProvider.builder()} habilita
     * el auto-refresh interno de credenciales STS/SSO, así el proceso no muere
     * cuando la sesión temporal expira (típico en dev con SSO/Identity Center).
     * En prod/QA — cuando corre en EC2 — cae al InstanceProfileCredentialsProvider
     * y el rol IAM del EC2 sirve credenciales sin fecha de expiración manual.
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.builder()
                .asyncCredentialUpdateEnabled(true)
                .build();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
