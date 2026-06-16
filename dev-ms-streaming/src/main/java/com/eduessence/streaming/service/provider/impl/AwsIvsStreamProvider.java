package com.eduessence.streaming.service.provider.impl;

import com.eduessence.streaming.exception.ServerApiStatusCode;
import com.eduessence.streaming.exception.StreamingApiException;
import com.eduessence.streaming.model.enums.ProviderTipo;
import com.eduessence.streaming.service.provider.StreamProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ivs.IvsClient;
import software.amazon.awssdk.services.ivs.model.Channel;
import software.amazon.awssdk.services.ivs.model.ChannelLatencyMode;
import software.amazon.awssdk.services.ivs.model.ChannelType;
import software.amazon.awssdk.services.ivs.model.CreateChannelRequest;
import software.amazon.awssdk.services.ivs.model.CreateChannelResponse;
import software.amazon.awssdk.services.ivs.model.DeleteChannelRequest;
import software.amazon.awssdk.services.ivs.model.DeleteStreamKeyRequest;
import software.amazon.awssdk.services.ivs.model.StreamKey;

/**
 * Implementación real con AWS IVS.
 * Crea un Channel + StreamKey por sesión y devuelve credenciales OBS + URL HLS.
 *
 * Costos: IVS cobra por minuto en vivo y por GB streaming. Para tests usar
 * channel type BASIC (más barato pero baja calidad).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eduessence.streaming.provider", havingValue = "AWS_IVS")
public class AwsIvsStreamProvider implements StreamProvider {

    private final IvsClient ivs;

    @Value("${eduessence.aws.ivs.channel-type:STANDARD}")
    private String channelType;

    @Value("${eduessence.aws.ivs.latency-mode:LOW}")
    private String latencyMode;

    @Override
    public ProviderTipo getTipo() {
        return ProviderTipo.AWS_IVS;
    }

    @Override
    public StreamCredentials crear(String titulo) {
        try {
            CreateChannelResponse res = ivs.createChannel(CreateChannelRequest.builder()
                    .name(titulo)
                    .type(ChannelType.fromValue(channelType))
                    .latencyMode(ChannelLatencyMode.fromValue(latencyMode))
                    .build());

            Channel channel = res.channel();
            StreamKey key = res.streamKey();

            // El ingestEndpoint viene sin protocolo: "xxx.global-contribute.live-video.net"
            String ingestUrl = "rtmps://" + channel.ingestEndpoint() + ":443/app/";

            return new StreamCredentials(
                    channel.arn(),
                    ingestUrl,
                    key.value(),
                    channel.playbackUrl()
            );
        } catch (Exception ex) {
            log.error("Error creando channel IVS: {}", ex.getMessage(), ex);
            throw new StreamingApiException(ServerApiStatusCode.PROVIDER_ERROR, ex);
        }
    }

    @Override
    public void eliminar(String providerResourceId) {
        try {
            // El providerResourceId es el channelArn. Borrar el channel borra también los stream keys.
            ivs.deleteChannel(DeleteChannelRequest.builder().arn(providerResourceId).build());
        } catch (Exception ex) {
            log.warn("Error eliminando channel {}: {}", providerResourceId, ex.getMessage());
        }
    }
}
