package com.eduessence.cursos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pool dedicado para broadcasts de correo (invitación a curso nuevo). Se aísla
 * del scheduler default para que un lote lento no bloquee el resto de tareas
 * (@Scheduled, requests HTTP, etc.).
 */
@Configuration
public class AsyncConfig {

    @Bean("broadcastExecutor")
    public Executor broadcastExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(20);
        ex.setThreadNamePrefix("broadcast-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();
        return ex;
    }
}
