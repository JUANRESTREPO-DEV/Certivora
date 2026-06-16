package com.eduessence.streaming.service.provider.impl;

import com.eduessence.streaming.model.enums.ProviderTipo;
import com.eduessence.streaming.service.provider.StreamProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Provider de desarrollo. Genera URLs falsas para que el front pueda probar
 * el flujo sin un servidor RTMP real.
 *
 * Activación: {@code eduessence.streaming.provider=MOCK} en application.properties
 * (default si no se especifica).
 */
@Component
@ConditionalOnProperty(name = "eduessence.streaming.provider", havingValue = "MOCK", matchIfMissing = true)
public class MockStreamProvider implements StreamProvider {

    private final SecureRandom random = new SecureRandom();

    @Override
    public ProviderTipo getTipo() {
        return ProviderTipo.MOCK;
    }

    @Override
    public StreamCredentials crear(String titulo) {
        String resourceId = UUID.randomUUID().toString();
        byte[] keyBytes = new byte[24];
        random.nextBytes(keyBytes);
        String streamKey = "mock_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        return new StreamCredentials(
                resourceId,
                "rtmp://mock.eduessence.local/live",
                streamKey,
                "https://mock.eduessence.local/live/" + resourceId + "/master.m3u8"
        );
    }

    @Override
    public void eliminar(String providerResourceId) {
        // no-op
    }
}
