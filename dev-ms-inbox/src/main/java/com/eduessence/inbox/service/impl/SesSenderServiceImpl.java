package com.eduessence.inbox.service.impl;

import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import com.eduessence.inbox.service.SesSenderService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesSenderServiceImpl implements SesSenderService {

    private final SesClient ses;

    @Override
    public String enviar(String fromAddress, String fromNombre,
                         List<String> to, List<String> cc, List<String> bcc,
                         String asunto, String cuerpoTexto, String cuerpoHtml,
                         String inReplyTo, List<String> referencias) {
        try {
            MimeMessage mime = new MimeMessage(Session.getDefaultInstance(new Properties()));
            mime.setFrom(new InternetAddress(fromAddress, fromNombre == null ? "" : fromNombre, StandardCharsets.UTF_8.name()));
            mime.setSentDate(new Date());
            mime.setSubject(asunto, StandardCharsets.UTF_8.name());

            for (String t : to) mime.addRecipient(Message.RecipientType.TO, new InternetAddress(t));
            if (cc != null) for (String c : cc) mime.addRecipient(Message.RecipientType.CC, new InternetAddress(c));
            if (bcc != null) for (String b : bcc) mime.addRecipient(Message.RecipientType.BCC, new InternetAddress(b));

            if (inReplyTo != null && !inReplyTo.isBlank()) mime.setHeader("In-Reply-To", inReplyTo);
            if (referencias != null && !referencias.isEmpty()) mime.setHeader("References", String.join(" ", referencias));

            MimeMultipart alt = new MimeMultipart("alternative");
            if (cuerpoTexto != null && !cuerpoTexto.isBlank()) {
                MimeBodyPart bpTexto = new MimeBodyPart();
                bpTexto.setText(cuerpoTexto, StandardCharsets.UTF_8.name());
                alt.addBodyPart(bpTexto);
            }
            if (cuerpoHtml != null && !cuerpoHtml.isBlank()) {
                MimeBodyPart bpHtml = new MimeBodyPart();
                bpHtml.setContent(cuerpoHtml, "text/html; charset=UTF-8");
                alt.addBodyPart(bpHtml);
            }
            mime.setContent(alt);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mime.writeTo(out);

            SendRawEmailResponse res = ses.sendRawEmail(SendRawEmailRequest.builder()
                    .rawMessage(RawMessage.builder()
                            .data(SdkBytes.fromByteArray(out.toByteArray()))
                            .build())
                    .build());
            log.info("SES enviado messageId={}", res.messageId());
            return res.messageId();
        } catch (Exception ex) {
            log.error("Error enviando via SES: {}", ex.getMessage(), ex);
            throw new InboxApiException(ServerApiStatusCode.ERROR_SES, ex);
        }
    }

    @Override
    public void forwardExternos(String fromAddress, List<String> destinos, byte[] emlOriginal, String asuntoPrefijo) {
        if (destinos == null || destinos.isEmpty()) return;
        try {
            // Strategy simple: enviar el .eml original como adjunto rfc822 con un mensaje pequeño.
            // (Reescribir headers para impersonar al remitente origina rompe DKIM y suele caer en spam.)
            MimeMessage cobertura = new MimeMessage(Session.getDefaultInstance(new Properties()));
            cobertura.setFrom(new InternetAddress(fromAddress));
            cobertura.setSubject((asuntoPrefijo == null ? "[Forward] " : asuntoPrefijo) + "correo corporativo", StandardCharsets.UTF_8.name());
            for (String d : destinos) cobertura.addRecipient(Message.RecipientType.TO, new InternetAddress(d));

            MimeMultipart mp = new MimeMultipart();
            MimeBodyPart texto = new MimeBodyPart();
            texto.setText("Mensaje reenviado automáticamente desde " + fromAddress
                    + "\nVea el original adjunto (.eml).", StandardCharsets.UTF_8.name());
            mp.addBodyPart(texto);

            MimeBodyPart adjunto = new MimeBodyPart();
            adjunto.setContent(emlOriginal, "message/rfc822");
            adjunto.setFileName("original.eml");
            mp.addBodyPart(adjunto);

            cobertura.setContent(mp);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            cobertura.writeTo(out);

            ses.sendRawEmail(SendRawEmailRequest.builder()
                    .rawMessage(RawMessage.builder().data(SdkBytes.fromByteArray(out.toByteArray())).build())
                    .build());
        } catch (Exception ex) {
            log.warn("Forward externo falló: {}", ex.getMessage());
        }
    }
}
