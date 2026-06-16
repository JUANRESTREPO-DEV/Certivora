package com.eduessence.inbox.service.impl;

import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import com.eduessence.inbox.service.MimeParserService;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class MimeParserServiceImpl implements MimeParserService {

    @Override
    public ParsedMessage parsear(byte[] eml) throws IOException {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mime = new MimeMessage(session, new ByteArrayInputStream(eml));

            String messageId = mime.getMessageID();
            String inReplyTo = primerHeader(mime, "In-Reply-To");
            List<String> refs = headersMultiples(mime, "References");

            InternetAddress fromAddr = (InternetAddress) (mime.getFrom() != null && mime.getFrom().length > 0 ? mime.getFrom()[0] : null);
            String fromEmail = fromAddr == null ? "" : fromAddr.getAddress();
            String fromNombre = fromAddr == null ? "" : (fromAddr.getPersonal() == null ? "" : fromAddr.getPersonal());

            List<String> to = direcciones(mime.getRecipients(Message.RecipientType.TO));
            List<String> cc = direcciones(mime.getRecipients(Message.RecipientType.CC));
            List<String> bcc = direcciones(mime.getRecipients(Message.RecipientType.BCC));

            String asunto = mime.getSubject() == null ? "(sin asunto)" : mime.getSubject();

            LocalDateTime fecha = mime.getSentDate() == null
                    ? LocalDateTime.now()
                    : LocalDateTime.ofInstant(mime.getSentDate().toInstant(), ZoneId.systemDefault());

            BuilderTextoHtml builder = new BuilderTextoHtml();
            List<Adjunto> adjuntos = new ArrayList<>();
            extraerBody(mime, builder, adjuntos);

            return new ParsedMessage(
                    messageId, inReplyTo, refs,
                    fromEmail, fromNombre,
                    to, cc, bcc,
                    asunto,
                    builder.texto.toString().trim(),
                    builder.html.toString().trim(),
                    fecha,
                    adjuntos
            );
        } catch (Exception ex) {
            log.error("Error parseando MIME: {}", ex.getMessage(), ex);
            throw new InboxApiException(ServerApiStatusCode.ERROR_PARSEO_MIME, ex);
        }
    }

    private void extraerBody(Part part, BuilderTextoHtml b, List<Adjunto> adjuntos) throws Exception {
        if (part.isMimeType("text/plain") && (part.getDisposition() == null || !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))) {
            Object c = part.getContent();
            if (c != null) b.texto.append(c);
        } else if (part.isMimeType("text/html") && (part.getDisposition() == null || !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))) {
            Object c = part.getContent();
            if (c != null) b.html.append(c);
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                extraerBody(bp, b, adjuntos);
            }
        } else if (part.getDisposition() != null && (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || Part.INLINE.equalsIgnoreCase(part.getDisposition()))) {
            String nombre = part.getFileName();
            if (nombre == null) nombre = "adjunto-" + (adjuntos.size() + 1);
            try (InputStream in = part.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                adjuntos.add(new Adjunto(nombre, part.getContentType(), out.toByteArray()));
            }
        }
    }

    private static List<String> direcciones(Address[] arr) {
        if (arr == null) return List.of();
        return Arrays.stream(arr)
                .filter(a -> a instanceof InternetAddress)
                .map(a -> ((InternetAddress) a).getAddress())
                .toList();
    }

    private static String primerHeader(MimeMessage m, String h) throws Exception {
        String[] v = m.getHeader(h);
        return v == null || v.length == 0 ? null : v[0];
    }

    private static List<String> headersMultiples(MimeMessage m, String h) throws Exception {
        String[] v = m.getHeader(h);
        if (v == null) return List.of();
        return Arrays.stream(v).flatMap(s -> Arrays.stream(s.split("\\s+"))).toList();
    }

    private static class BuilderTextoHtml {
        StringBuilder texto = new StringBuilder();
        StringBuilder html = new StringBuilder();
    }
}
