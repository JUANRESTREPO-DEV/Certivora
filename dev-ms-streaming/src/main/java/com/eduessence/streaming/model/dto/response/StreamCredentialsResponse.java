package com.eduessence.streaming.model.dto.response;

import com.eduessence.streaming.model.enums.ProviderTipo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lo que el instructor necesita para configurar OBS:
 *  - {@code ingestUrl} → "Server" en OBS
 *  - {@code streamKey} → "Stream Key" en OBS
 *
 * Lo que necesita el viewer:
 *  - {@code playbackUrl} (URL HLS .m3u8)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamCredentialsResponse {
    private Long sessionId;
    private ProviderTipo provider;
    private String ingestUrl;
    private String streamKey;
    private String playbackUrl;
}
