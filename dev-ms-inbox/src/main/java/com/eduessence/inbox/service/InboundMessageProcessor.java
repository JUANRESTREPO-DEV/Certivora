package com.eduessence.inbox.service;

import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import com.eduessence.inbox.model.entity.Buzon;
import com.eduessence.inbox.model.entity.Mensaje;
import com.eduessence.inbox.model.entity.MensajeAdjunto;
import com.eduessence.inbox.model.enums.Carpeta;
import com.eduessence.inbox.model.enums.EstadoMensaje;
import com.eduessence.inbox.model.enums.TipoBuzon;
import com.eduessence.inbox.repository.MensajeAdjuntoRepository;
import com.eduessence.inbox.repository.MensajeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Procesa cada notificación de SES Inbound entregada por SNS → SQS.
 *
 *   1. Descarga el .eml del bucket bruto
 *   2. Parsea MIME
 *   3. Resuelve el buzón por header To: (incluye alias y catch-all)
 *   4. Persiste mensaje + adjuntos (en bucket de adjuntos)
 *   5. Si el buzón tiene forward_externos, reenvía vía SES
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundMessageProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final S3StorageService s3;
    private final MimeParserService parser;
    private final BuzonService buzonService;
    private final SesSenderService sesSender;
    private final MensajeRepository mensajeRepo;
    private final MensajeAdjuntoRepository adjuntoRepo;

    @Value("${eduessence.inbox.forward.from-default}")
    private String fromForwardDefault;

    @Transactional
    public void procesarNotificacionSes(String sqsBodyJson) {
        try {
            var sns = MAPPER.readTree(sqsBodyJson);
            // SNS envuelve el mensaje en "Message" como string JSON
            String messageStr = sns.has("Message") ? sns.get("Message").asText() : sqsBodyJson;
            var n = MAPPER.readTree(messageStr);

            String emlKey = extraerEmlKey(n);
            if (emlKey == null) {
                log.warn("Notificación SES sin objectKey, se ignora: {}", messageStr);
                return;
            }

            byte[] eml = s3.descargarRaw(emlKey);
            var parsed = parser.parsear(eml);

            for (String destinatario : parsed.to()) {
                Buzon buzon = buzonService.resolverBuzonPorDireccion(destinatario);
                if (buzon == null) {
                    log.info("Sin buzón para {}, se ignora", destinatario);
                    continue;
                }
                if (buzon.getTipo() == TipoBuzon.SOLO_SALIDA) {
                    log.info("Buzón {} es SOLO_SALIDA, se ignora", buzon.getDireccion());
                    continue;
                }

                // Idempotencia: si ya existe el messageId, no duplicar
                if (parsed.messageId() != null && mensajeRepo.findByMessageId(parsed.messageId()).isPresent()) {
                    log.info("Mensaje {} ya estaba persistido", parsed.messageId());
                    continue;
                }

                String threadId = primerNoNulo(parsed.references().stream().findFirst().orElse(null),
                        parsed.inReplyTo(),
                        parsed.messageId());

                Mensaje mensaje = Mensaje.builder()
                        .buzon(buzon)
                        .messageId(parsed.messageId() == null ? "<gen-" + UUID.randomUUID() + ">" : parsed.messageId())
                        .threadId(threadId)
                        .inReplyTo(parsed.inReplyTo())
                        .remitenteEmail(parsed.fromEmail())
                        .remitenteNombre(parsed.fromNombre())
                        .destinatariosJson(MAPPER.writeValueAsString(unir(parsed.to(), parsed.cc())))
                        .asunto(parsed.asunto())
                        .snippet(truncar(parsed.cuerpoTexto() != null && !parsed.cuerpoTexto().isBlank()
                                ? parsed.cuerpoTexto() : stripHtml(parsed.cuerpoHtml()), 280))
                        .cuerpoTexto(parsed.cuerpoTexto())
                        .cuerpoHtml(parsed.cuerpoHtml())
                        .s3EmlKey(emlKey)
                        .fechaRecibido(parsed.fechaRecibido())
                        .carpeta(Carpeta.INBOX)
                        .estado(EstadoMensaje.RECIBIDO)
                        .leido(false)
                        .destacado(false)
                        .tieneAdjuntos(!parsed.adjuntos().isEmpty())
                        .tamanoBytes((long) eml.length)
                        .build();
                mensaje = mensajeRepo.save(mensaje);

                // Adjuntos a S3
                for (var a : parsed.adjuntos()) {
                    String key = "adjuntos/" + mensaje.getId() + "/" + sanitize(a.nombre());
                    s3.subirAdjunto(key, a.datos(), a.contentType());
                    adjuntoRepo.save(MensajeAdjunto.builder()
                            .mensaje(mensaje)
                            .nombre(a.nombre())
                            .contentType(a.contentType())
                            .tamanoBytes((long) a.datos().length)
                            .s3Key(key)
                            .build());
                }

                // Forward a externos si está configurado
                List<String> forwards = deserializarLista(buzon.getForwardExternosJson());
                if (!forwards.isEmpty()) {
                    String from = buzon.getDireccion() != null ? buzon.getDireccion() : fromForwardDefault;
                    sesSender.forwardExternos(from, forwards, eml, "[Fwd " + buzon.getDireccion() + "] ");
                }
            }

        } catch (InboxApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error procesando notificación SES: {}", ex.getMessage(), ex);
            throw new InboxApiException(ServerApiStatusCode.ERROR_INTERNO, ex);
        }
    }

    /** En notificaciones SES → SNS la key viene como receipt.action.objectKey. */
    private static String extraerEmlKey(com.fasterxml.jackson.databind.JsonNode n) {
        if (n.has("receipt") && n.get("receipt").has("action")
                && n.get("receipt").get("action").has("objectKey"))
            return n.get("receipt").get("action").get("objectKey").asText();
        // formato directo
        if (n.has("objectKey")) return n.get("objectKey").asText();
        return null;
    }

    private static String primerNoNulo(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static List<String> unir(List<String>... listas) {
        List<String> r = new ArrayList<>();
        for (var l : listas) if (l != null) r.addAll(l);
        return r;
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String truncar(String s, int n) {
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static List<String> deserializarLista(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
