package com.eduessence.inbox.service;

import java.io.IOException;
import java.util.List;

public interface MimeParserService {

    record ParsedMessage(
            String messageId,
            String inReplyTo,
            List<String> references,
            String fromEmail,
            String fromNombre,
            List<String> to,
            List<String> cc,
            List<String> bcc,
            String asunto,
            String cuerpoTexto,
            String cuerpoHtml,
            java.time.LocalDateTime fechaRecibido,
            List<Adjunto> adjuntos
    ) {}

    record Adjunto(String nombre, String contentType, byte[] datos) {}

    ParsedMessage parsear(byte[] eml) throws IOException;
}
