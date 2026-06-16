package com.eduessence.sendmail.service;

import com.eduessence.sendmail.model.dto.EmailResultDTO;
import com.eduessence.sendmail.model.dto.EnviarConAdjuntoRequest;
import com.eduessence.sendmail.model.dto.EnviarEmailRequest;

public interface EmailService {

    /** Envía 1 email a 1 destinatario usando un template Velocity descargado de S3. */
    EmailResultDTO enviarDinamico(EnviarEmailRequest request);

    /**
     * Envía a N destinatarios. Si {@code nombreTemplate} viene, renderiza Velocity;
     * si no, usa {@code cuerpoHtml} tal cual. Soporta adjuntos en base64.
     */
    EmailResultDTO enviarConAdjuntos(EnviarConAdjuntoRequest request);
}
