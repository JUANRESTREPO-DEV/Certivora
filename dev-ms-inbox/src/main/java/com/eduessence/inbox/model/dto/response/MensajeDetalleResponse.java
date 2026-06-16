package com.eduessence.inbox.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeDetalleResponse {
    private Long id;
    private Long buzonId;
    private String buzonDireccion;
    private String messageId;
    private String threadId;
    private String inReplyTo;
    private String remitenteEmail;
    private String remitenteNombre;
    private List<String> destinatarios;
    private String asunto;
    private String cuerpoHtml;
    private String cuerpoTexto;
    private LocalDateTime fechaRecibido;
    private Boolean leido;
    private List<AdjuntoDTO> adjuntos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjuntoDTO {
        private Long id;
        private String nombre;
        private String contentType;
        private Long tamanoBytes;
        /** URL pre-firmada al adjunto en S3 (válida 1h). */
        private String urlDescarga;
    }
}
