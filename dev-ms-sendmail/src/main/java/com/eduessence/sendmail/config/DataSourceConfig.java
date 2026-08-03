package com.eduessence.sendmail.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Configura el {@link DataSource} programáticamente cuando se necesite obtener
 * la password desde AWS Secrets Manager (perfil {@code prod}).
 *
 * Para activarlo: descomenta la anotación {@code @Configuration}.
 * En {@code dev} y {@code qa} la password viene del .env, no hace falta
 * activar esta clase — Spring Boot crea el DataSource automáticamente.
 */
@Configuration
@Profile("prod")
public class DataSourceConfig {

    @Autowired
    private Environment env;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.secretsmanager.secretName}")
    private String secretName;

    @Bean
    @Primary
    public DataSource dataSource() {
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");

        String password = null;
        if (isProd) {
            password = getSecretPassword(secretName);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("SendmailHikariPool");

        return new HikariDataSource(config);
    }

    private String getSecretPassword(String secretName) {
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        GetSecretValueRequest req = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        GetSecretValueResponse res = client.getSecretValue(req);

        JSONObject secretJson = new JSONObject(res.secretString());
        return secretJson.getString("RDS_PASSWORD");
    }
}
