package com.eduessence.sendmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SendmailApplication {
    public static void main(String[] args) {
        SpringApplication.run(SendmailApplication.class, args);
    }
}
