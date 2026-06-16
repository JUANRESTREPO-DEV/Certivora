package com.eduessence.inbox.config;

/*
 * Configuracion comentada de HikariCP + AWS Secrets Manager.
 *
 * Descomenta @Configuration en prod para que las credenciales DB salgan de
 * Secrets Manager (secret eduessence/prod/rds) y no de variables de entorno.
 */
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.zaxxer.hikari.HikariConfig;
// import com.zaxxer.hikari.HikariDataSource;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
// import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
//
// import javax.sql.DataSource;
//
// //@Configuration
// public class DataSourceConfig {
//
//     @Value("${aws.region}")
//     private String region;
//
//     @Value("${aws.secretsmanager.secretName}")
//     private String secretName;
//
//     @Bean
//     public DataSource dataSource() throws Exception {
//         try (SecretsManagerClient sm = SecretsManagerClient.builder()
//                 .region(Region.of(region)).build()) {
//             var secret = sm.getSecretValue(GetSecretValueRequest.builder()
//                     .secretId(secretName).build()).secretString();
//             var node = new ObjectMapper().readTree(secret);
//
//             HikariConfig cfg = new HikariConfig();
//             cfg.setJdbcUrl("jdbc:mysql://" + node.get("host").asText() + ":" +
//                     node.get("port").asText() + "/" + node.get("db").asText() +
//                     "?useSSL=true&serverTimezone=America/Bogota");
//             cfg.setUsername(node.get("username").asText());
//             cfg.setPassword(node.get("password").asText());
//             cfg.setMaximumPoolSize(10);
//             cfg.setPoolName("InboxHikariPool");
//             return new HikariDataSource(cfg);
//         }
//     }
// }
