package com.eduessence.sendmail.service;

import com.eduessence.sendmail.exception.SendmailApiException;
import com.eduessence.sendmail.exception.ServerApiStatusCode;
import com.eduessence.sendmail.model.dto.AdjuntoDTO;
import com.eduessence.sendmail.model.dto.DestinatarioDTO;
import com.eduessence.sendmail.model.entity.EmailTemplate;
import com.eduessence.sendmail.model.entity.EmailTemplateVariable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Helper compartido entre {@code EmailService} (envío real) y
 * {@code TemplateService} (preview + test-send admin). Centraliza:
 *
 *   - construcción del contexto Velocity (variables del request + defaults
 *     del template + variables del sistema/empresa);
 *   - validación de variables requeridas;
 *   - render Velocity sobre cualquier string HTML;
 *   - envío de un MimeMessage (con o sin adjuntos).
 *
 * Lo agregamos como bean separado para evitar la dependencia circular que
 * tendríamos si {@code TemplateServiceImpl} inyectara {@code EmailService}
 * (y viceversa).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRenderer {

    private static final ZoneId ZONE_BOGOTA = ZoneId.of("America/Bogota");
    private static final Locale LOCALE_ES = new Locale("es", "CO");

    private final VelocityEngine velocity;
    private final JavaMailSender mailSender;

    @Value("${eduessence.empresa.nombre:Eduessence}")
    private String empresaNombre;

    @Value("${eduessence.empresa.correo:contacto@eduessence.com}")
    private String empresaCorreo;

    @Value("${eduessence.empresa.sitio-web:https://eduessence.com}")
    private String empresaSitioWeb;

    @Value("${eduessence.empresa.telefono:}")
    private String empresaTelefono;

    @Value("${eduessence.empresa.logo-url:}")
    private String empresaLogoUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    /**
     * Construye el contexto Velocity mezclando variables del request +
     * defaults del template + variables fijas del sistema (fecha, empresa).
     */
    public Map<String, Object> construirContexto(EmailTemplate template,
                                                 DestinatarioDTO destinatario,
                                                 Map<String, Object> variablesUsuario) {
        Map<String, Object> ctx = new HashMap<>(
                variablesUsuario == null ? Map.of() : variablesUsuario);

        // Destinatario
        if (destinatario != null) {
            ctx.put("destinatarioCorreo", nullSafe(destinatario.getCorreo()));
            ctx.put("destinatarioNombre", nullSafe(destinatario.getNombre()));
            ctx.putIfAbsent("nombreDestinatario", nullSafe(destinatario.getNombre()));
        }

        // Sistema
        LocalDateTime now = LocalDateTime.now(ZONE_BOGOTA);
        ctx.put("fecha", now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        ctx.put("fechaActual",
                now.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", LOCALE_ES)));
        ctx.put("horaActual", now.format(DateTimeFormatter.ofPattern("HH:mm")));
        ctx.put("anioActual", now.getYear());

        // Empresa
        ctx.put("empresaNombre", empresaNombre);
        ctx.put("empresaCorreo", empresaCorreo);
        ctx.put("empresaSitioWeb", empresaSitioWeb);
        ctx.put("empresaTelefono", empresaTelefono);
        ctx.put("empresaLogoUrl", empresaLogoUrl);

        if (!isBlank(empresaLogoUrl)) {
            ctx.put("logoContentId", UUID.randomUUID().toString());
        }

        // Defaults declarados por el template
        if (template != null && template.getVariables() != null) {
            for (EmailTemplateVariable v : template.getVariables()) {
                if (!ctx.containsKey(v.getNombre()) && !isBlank(v.getValorDefecto())) {
                    ctx.put(v.getNombre(), v.getValorDefecto());
                }
            }
        }
        return ctx;
    }

    /** Verifica que todas las variables {@code requerido=true} estén presentes. */
    public void validarRequeridas(EmailTemplate template, Map<String, Object> ctx) {
        for (String falta : variablesFaltantes(template, ctx)) {
            throw new SendmailApiException(ServerApiStatusCode.VARIABLE_REQUERIDA_FALTANTE,
                    "Falta la variable requerida: " + falta);
        }
    }

    /**
     * Devuelve qué variables requeridas no están presentes en el contexto.
     * No lanza excepción — la lista vacía significa que todo está OK.
     */
    public List<String> variablesFaltantes(EmailTemplate template, Map<String, Object> ctx) {
        List<String> faltantes = new ArrayList<>();
        if (template == null || template.getVariables() == null) return faltantes;
        for (EmailTemplateVariable v : template.getVariables()) {
            if (!Boolean.TRUE.equals(v.getRequerido())) continue;
            Object val = ctx.get(v.getNombre());
            if (val == null || (val instanceof String s && s.isBlank())) {
                faltantes.add(v.getNombre());
            }
        }
        return faltantes;
    }

    /** Renderiza el HTML aplicando Velocity con el contexto. */
    public String render(String html, Map<String, Object> ctx) {
        if (html == null || html.isBlank()) {
            throw new SendmailApiException(ServerApiStatusCode.ERROR_RENDER_TEMPLATE,
                    "Template vacío");
        }
        try {
            VelocityContext vctx = new VelocityContext(ctx);
            StringWriter writer = new StringWriter();
            boolean ok = velocity.evaluate(vctx, writer, "EmailTemplate", new StringReader(html));
            if (!ok) {
                throw new SendmailApiException(ServerApiStatusCode.ERROR_RENDER_TEMPLATE,
                        "Velocity no pudo evaluar el template");
            }
            return writer.toString();
        } catch (SendmailApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SendmailApiException(ServerApiStatusCode.ERROR_RENDER_TEMPLATE, ex);
        }
    }

    /** Envía un correo HTML (puede incluir adjuntos). */
    public void enviarMime(String correo, String asunto, String html,
                           String logoContentId, List<AdjuntoDTO> adjuntos)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        boolean multipart = (adjuntos != null && !adjuntos.isEmpty())
                || !isBlank(logoContentId);
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");

        helper.setTo(correo);
        helper.setSubject(asunto == null ? "" : asunto);
        helper.setFrom(new InternetAddress(mailFrom, empresaNombre));
        helper.setText(html, true);

        if (adjuntos != null) {
            for (AdjuntoDTO a : adjuntos) {
                byte[] contenido = Base64.getDecoder().decode(a.getContenidoBase64());
                helper.addAttachment(a.getNombreArchivo(),
                        new ByteArrayResource(contenido), a.getContentType());
            }
        }
        mailSender.send(message);
        log.info("Correo enviado a {} (asunto: {})", correo, asunto);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String nullSafe(String s) { return s == null ? "" : s; }
}
