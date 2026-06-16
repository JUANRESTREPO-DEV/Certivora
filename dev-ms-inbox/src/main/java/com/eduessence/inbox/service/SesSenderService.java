package com.eduessence.inbox.service;

import java.util.List;

public interface SesSenderService {

    /**
     * Envía un correo nuevo desde una dirección corporativa verificada.
     * Devuelve el Message-ID asignado por SES.
     */
    String enviar(String fromAddress, String fromNombre,
                  List<String> to, List<String> cc, List<String> bcc,
                  String asunto, String cuerpoTexto, String cuerpoHtml,
                  String inReplyTo, List<String> referencias);

    /**
     * Reenvía un .eml ya parseado a destinatarios externos manteniendo
     * el contenido original (útil para forward a Gmail/Outlook personal).
     */
    void forwardExternos(String fromAddress, List<String> destinos, byte[] emlOriginal, String asuntoPrefijo);
}
