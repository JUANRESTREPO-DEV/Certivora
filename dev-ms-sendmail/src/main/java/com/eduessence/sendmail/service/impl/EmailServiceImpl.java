package com.eduessence.sendmail.service.impl;

import com.eduessence.sendmail.exception.SendmailApiException;
import com.eduessence.sendmail.exception.ServerApiStatusCode;
import com.eduessence.sendmail.model.dto.DestinatarioDTO;
import com.eduessence.sendmail.model.dto.EmailResultDTO;
import com.eduessence.sendmail.model.dto.EnviarConAdjuntoRequest;
import com.eduessence.sendmail.model.dto.EnviarEmailRequest;
import com.eduessence.sendmail.model.entity.EmailTemplate;
import com.eduessence.sendmail.model.enums.EstadoEnvio;
import com.eduessence.sendmail.service.EmailLogService;
import com.eduessence.sendmail.service.EmailRenderer;
import com.eduessence.sendmail.service.EmailService;
import com.eduessence.sendmail.service.S3TemplateLoader;
import com.eduessence.sendmail.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Render dinámico de emails desde templates en S3.
 *
 * Flujo {@link #enviarDinamico(EnviarEmailRequest)}:
 *  1. Busca el template activo por nombre en BD.
 *  2. Descarga el HTML desde S3 (key = {@code path_template}).
 *  3. Arma el contexto Velocity con: variables del request + valores por
 *     defecto del template + variables del sistema (fecha, hora, empresa).
 *  4. Valida que las variables requeridas estén presentes.
 *  5. Renderiza con Velocity, envía con JavaMailSender y registra en
 *     {@code email_envio}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final TemplateService templateService;
    private final S3TemplateLoader s3Loader;
    private final EmailLogService logService;
    private final EmailRenderer renderer;

    /* ─────────────────────────── enviarDinamico ─────────────────────────── */

    @Override
    public EmailResultDTO enviarDinamico(EnviarEmailRequest req) {
        if (req.getDestinatario() == null || isBlank(req.getDestinatario().getCorreo())) {
            throw new SendmailApiException(ServerApiStatusCode.DESTINATARIOS_VACIOS);
        }

        EmailTemplate template = templateService.findActivoPorNombre(req.getNombreTemplate());
        String html = s3Loader.descargar(template.getBucket(), template.getPathTemplate());
        Map<String, Object> contexto = renderer.construirContexto(template, req.getDestinatario(),
                req.getVariables() == null ? Map.of() : req.getVariables());
        renderer.validarRequeridas(template, contexto);

        String asunto = isBlank(req.getAsunto()) ? template.getAsuntoDefault() : req.getAsunto();
        String htmlRender = renderer.render(html, contexto);

        String correo = req.getDestinatario().getCorreo().trim();
        try {
            renderer.enviarMime(correo, asunto, htmlRender,
                    (String) contexto.get("logoContentId"), null);
            logService.registrar(template, correo, req.getDestinatario().getNombre(), asunto,
                    contexto, EstadoEnvio.ENVIADO, null);
            return EmailResultDTO.builder()
                    .ok(true).mensaje("Correo enviado").exitosos(1).fallidos(0).build();
        } catch (Exception ex) {
            log.error("Error enviando a {}: {}", correo, ex.getMessage(), ex);
            logService.registrar(template, correo, req.getDestinatario().getNombre(), asunto,
                    contexto, EstadoEnvio.FALLIDO, ex.getMessage());
            throw new SendmailApiException(ServerApiStatusCode.ERROR_SMTP, ex);
        }
    }

    /* ──────────────────────── enviarConAdjuntos ──────────────────────── */

    @Override
    public EmailResultDTO enviarConAdjuntos(EnviarConAdjuntoRequest req) {
        if (req.getDestinatarios() == null || req.getDestinatarios().isEmpty()) {
            throw new SendmailApiException(ServerApiStatusCode.DESTINATARIOS_VACIOS);
        }

        boolean usaTemplate = !isBlank(req.getNombreTemplate());
        EmailTemplate template = null;
        String templateHtml = null;
        if (usaTemplate) {
            template = templateService.findActivoPorNombre(req.getNombreTemplate());
            templateHtml = s3Loader.descargar(template.getBucket(), template.getPathTemplate());
        }

        int ok = 0;
        int fail = 0;
        String asuntoDefault = template != null ? template.getAsuntoDefault() : req.getAsunto();
        String asunto = isBlank(req.getAsunto()) ? asuntoDefault : req.getAsunto();

        for (DestinatarioDTO d : req.getDestinatarios()) {
            if (isBlank(d.getCorreo())) {
                fail++;
                continue;
            }
            try {
                String html;
                String logoContentId = null;
                Map<String, Object> contexto = Map.of();
                if (usaTemplate) {
                    contexto = renderer.construirContexto(template, d,
                            req.getVariables() == null ? Map.of() : req.getVariables());
                    renderer.validarRequeridas(template, contexto);
                    html = renderer.render(templateHtml, contexto);
                    logoContentId = (String) contexto.get("logoContentId");
                } else {
                    html = req.getCuerpoHtml() == null ? "" : req.getCuerpoHtml();
                }
                renderer.enviarMime(d.getCorreo().trim(), asunto, html,
                        logoContentId, req.getAdjuntos());
                if (usaTemplate) {
                    logService.registrar(template, d.getCorreo(), d.getNombre(), asunto,
                            contexto, EstadoEnvio.ENVIADO, null);
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                log.error("Error enviando a {}: {}", d.getCorreo(), ex.getMessage());
                if (usaTemplate) {
                    logService.registrar(template, d.getCorreo(), d.getNombre(), asunto,
                            null, EstadoEnvio.FALLIDO, ex.getMessage());
                }
            }
        }
        return EmailResultDTO.builder()
                .ok(ok > 0)
                .mensaje(String.format("Enviados: %d, Fallidos: %d", ok, fail))
                .exitosos(ok).fallidos(fail).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
