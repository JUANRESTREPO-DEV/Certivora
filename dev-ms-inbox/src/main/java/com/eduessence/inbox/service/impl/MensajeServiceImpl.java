package com.eduessence.inbox.service.impl;

import com.eduessence.inbox.common.RequestContext;
import com.eduessence.inbox.exception.InboxApiException;
import com.eduessence.inbox.exception.ServerApiStatusCode;
import com.eduessence.inbox.model.dto.request.ComposerRequest;
import com.eduessence.inbox.model.dto.request.ResponderRequest;
import com.eduessence.inbox.model.dto.response.MensajeDetalleResponse;
import com.eduessence.inbox.model.dto.response.MensajeResponse;
import com.eduessence.inbox.model.entity.Buzon;
import com.eduessence.inbox.model.entity.Mensaje;
import com.eduessence.inbox.model.entity.MensajeAdjunto;
import com.eduessence.inbox.model.enums.Carpeta;
import com.eduessence.inbox.model.enums.EstadoMensaje;
import com.eduessence.inbox.model.enums.RolBuzon;
import com.eduessence.inbox.repository.BuzonAccesoRepository;
import com.eduessence.inbox.repository.BuzonRepository;
import com.eduessence.inbox.repository.MensajeAdjuntoRepository;
import com.eduessence.inbox.repository.MensajeRepository;
import com.eduessence.inbox.service.MensajeService;
import com.eduessence.inbox.service.S3StorageService;
import com.eduessence.inbox.service.SesSenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MensajeServiceImpl implements MensajeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long URL_TTL_ADJUNTO = 60 * 60L; // 1h

    private final MensajeRepository mensajeRepo;
    private final MensajeAdjuntoRepository adjuntoRepo;
    private final BuzonRepository buzonRepo;
    private final BuzonAccesoRepository accesoRepo;
    private final SesSenderService sesSender;
    private final S3StorageService s3;
    private final RequestContext ctx;

    @Override
    @Transactional(readOnly = true)
    public Page<MensajeResponse> listarDeBuzon(Long buzonId, Carpeta carpeta, Long usuarioActual, Pageable pageable) {
        requireAcceso(buzonId, usuarioActual, RolBuzon.LECTOR);
        Carpeta cp = carpeta == null ? Carpeta.INBOX : carpeta;
        return mensajeRepo.findAllByBuzonIdAndCarpetaOrderByFechaRecibidoDesc(buzonId, cp, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MensajeResponse> buscar(Long buzonId, String q, Long usuarioActual, Pageable pageable) {
        requireAcceso(buzonId, usuarioActual, RolBuzon.LECTOR);
        return mensajeRepo.buscar(buzonId, q == null ? "" : q.trim(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public MensajeDetalleResponse obtener(Long mensajeId, Long usuarioActual) {
        Mensaje m = getMensajeOrThrow(mensajeId);
        requireAcceso(m.getBuzon().getId(), usuarioActual, RolBuzon.LECTOR);

        // Marcar leído al primer acceso
        if (!m.getLeido()) {
            m.setLeido(true);
            m.setEstado(EstadoMensaje.LEIDO);
            mensajeRepo.save(m);
        }

        List<MensajeDetalleResponse.AdjuntoDTO> adjuntos = adjuntoRepo.findAllByMensajeId(mensajeId).stream()
                .map(a -> MensajeDetalleResponse.AdjuntoDTO.builder()
                        .id(a.getId())
                        .nombre(a.getNombre())
                        .contentType(a.getContentType())
                        .tamanoBytes(a.getTamanoBytes())
                        .urlDescarga(s3.urlPreFirmadaAdjunto(stripS3Prefix(a.getS3Key()), URL_TTL_ADJUNTO))
                        .build())
                .toList();

        return MensajeDetalleResponse.builder()
                .id(m.getId())
                .buzonId(m.getBuzon().getId())
                .buzonDireccion(m.getBuzon().getDireccion())
                .messageId(m.getMessageId())
                .threadId(m.getThreadId())
                .inReplyTo(m.getInReplyTo())
                .remitenteEmail(m.getRemitenteEmail())
                .remitenteNombre(m.getRemitenteNombre())
                .destinatarios(deserializarLista(m.getDestinatariosJson()))
                .asunto(m.getAsunto())
                .cuerpoHtml(m.getCuerpoHtml())
                .cuerpoTexto(m.getCuerpoTexto())
                .fechaRecibido(m.getFechaRecibido())
                .leido(m.getLeido())
                .adjuntos(adjuntos)
                .build();
    }

    @Override
    @Transactional
    public void marcarLeido(Long mensajeId, boolean leido, Long usuarioActual) {
        Mensaje m = getMensajeOrThrow(mensajeId);
        requireAcceso(m.getBuzon().getId(), usuarioActual, RolBuzon.LECTOR);
        m.setLeido(leido);
        m.setEstado(leido ? EstadoMensaje.LEIDO : EstadoMensaje.RECIBIDO);
        mensajeRepo.save(m);
    }

    @Override
    @Transactional
    public void mover(Long mensajeId, Carpeta carpeta, Long usuarioActual) {
        Mensaje m = getMensajeOrThrow(mensajeId);
        requireAcceso(m.getBuzon().getId(), usuarioActual, RolBuzon.RESPONDER);
        m.setCarpeta(carpeta);
        if (carpeta == Carpeta.ARCHIVADOS) m.setEstado(EstadoMensaje.ARCHIVADO);
        if (carpeta == Carpeta.PAPELERA) m.setEstado(EstadoMensaje.ELIMINADO);
        if (carpeta == Carpeta.SPAM) m.setEstado(EstadoMensaje.SPAM);
        mensajeRepo.save(m);
    }

    @Override
    @Transactional
    public void destacar(Long mensajeId, boolean destacado, Long usuarioActual) {
        Mensaje m = getMensajeOrThrow(mensajeId);
        requireAcceso(m.getBuzon().getId(), usuarioActual, RolBuzon.LECTOR);
        m.setDestacado(destacado);
        mensajeRepo.save(m);
    }

    @Override
    @Transactional
    public Long responder(Long mensajeId, ResponderRequest req, Long usuarioActual) {
        Mensaje original = getMensajeOrThrow(mensajeId);
        Buzon buzon = original.getBuzon();
        requireAcceso(buzon.getId(), usuarioActual, RolBuzon.RESPONDER);

        String asunto = original.getAsunto() == null ? "(sin asunto)" : original.getAsunto();
        if (!asunto.toUpperCase().startsWith("RE:")) asunto = "RE: " + asunto;

        List<String> to = new ArrayList<>(List.of(original.getRemitenteEmail()));
        List<String> cc = new ArrayList<>();
        if (Boolean.TRUE.equals(req.getResponderATodos())) {
            // todos los destinatarios originales que NO sean este buzón
            for (String d : deserializarLista(original.getDestinatariosJson())) {
                if (!d.equalsIgnoreCase(buzon.getDireccion())) cc.add(d);
            }
        }
        if (req.getCc() != null) cc.addAll(req.getCc());

        String cuerpoTexto = req.getCuerpoTexto();
        String cuerpoHtml = req.getCuerpoHtml();
        if (buzon.getFirma() != null && !buzon.getFirma().isBlank() && cuerpoHtml != null) {
            cuerpoHtml = cuerpoHtml + "<br><br>--<br>" + buzon.getFirma();
        }

        List<String> referencias = new ArrayList<>();
        if (original.getInReplyTo() != null) referencias.add(original.getInReplyTo());
        if (original.getMessageId() != null) referencias.add(original.getMessageId());

        String newMessageId = sesSender.enviar(
                buzon.getDireccion(), buzon.getNombreMostrar(),
                to, cc, req.getBcc(),
                asunto, cuerpoTexto, cuerpoHtml,
                original.getMessageId(), referencias);

        // Guarda el saliente en carpeta ENVIADOS
        Mensaje saliente = Mensaje.builder()
                .buzon(buzon)
                .messageId(newMessageId)
                .threadId(original.getThreadId() == null ? original.getMessageId() : original.getThreadId())
                .inReplyTo(original.getMessageId())
                .remitenteEmail(buzon.getDireccion())
                .remitenteNombre(buzon.getNombreMostrar())
                .destinatariosJson(serializarLista(unirListas(to, cc, req.getBcc())))
                .asunto(asunto)
                .snippet(truncar(cuerpoTexto != null ? cuerpoTexto : stripHtml(cuerpoHtml), 280))
                .cuerpoTexto(cuerpoTexto)
                .cuerpoHtml(cuerpoHtml)
                .fechaRecibido(LocalDateTime.now())
                .carpeta(Carpeta.ENVIADOS)
                .estado(EstadoMensaje.RESPONDIDO)
                .leido(true)
                .enviadoPorUsuarioId(usuarioActual)
                .build();
        saliente = mensajeRepo.save(saliente);

        original.setEstado(EstadoMensaje.RESPONDIDO);
        mensajeRepo.save(original);

        return saliente.getId();
    }

    @Override
    @Transactional
    public Long componer(ComposerRequest req, Long usuarioActual) {
        Buzon buzon = buzonRepo.findById(req.getBuzonId())
                .orElseThrow(() -> new InboxApiException(ServerApiStatusCode.BUZON_NO_ENCONTRADO));
        requireAcceso(buzon.getId(), usuarioActual, RolBuzon.RESPONDER);
        if (!buzon.getActivo())
            throw new InboxApiException(ServerApiStatusCode.BUZON_INACTIVO);

        String cuerpoHtml = req.getCuerpoHtml();
        if (buzon.getFirma() != null && !buzon.getFirma().isBlank() && cuerpoHtml != null) {
            cuerpoHtml = cuerpoHtml + "<br><br>--<br>" + buzon.getFirma();
        }

        String messageId = sesSender.enviar(
                buzon.getDireccion(), buzon.getNombreMostrar(),
                req.getTo(), req.getCc(), req.getBcc(),
                req.getAsunto(), req.getCuerpoTexto(), cuerpoHtml,
                null, null);

        Mensaje saliente = Mensaje.builder()
                .buzon(buzon)
                .messageId(messageId)
                .threadId(messageId)
                .remitenteEmail(buzon.getDireccion())
                .remitenteNombre(buzon.getNombreMostrar())
                .destinatariosJson(serializarLista(unirListas(req.getTo(), req.getCc(), req.getBcc())))
                .asunto(req.getAsunto())
                .snippet(truncar(req.getCuerpoTexto() != null ? req.getCuerpoTexto() : stripHtml(cuerpoHtml), 280))
                .cuerpoTexto(req.getCuerpoTexto())
                .cuerpoHtml(cuerpoHtml)
                .fechaRecibido(LocalDateTime.now())
                .carpeta(Carpeta.ENVIADOS)
                .estado(EstadoMensaje.RESPONDIDO)
                .leido(true)
                .enviadoPorUsuarioId(usuarioActual)
                .build();
        return mensajeRepo.save(saliente).getId();
    }

    /* ───────── helpers ───────── */

    private void requireAcceso(Long buzonId, Long usuarioActual, RolBuzon rolMinimo) {
        // ADMIN/GERENTE siempre pasan
        if (ctx.esAdminOGerente()) return;

        var acceso = accesoRepo.findByBuzonIdAndUsuarioId(buzonId, usuarioActual)
                .orElseThrow(() -> new InboxApiException(ServerApiStatusCode.ACCESO_DENEGADO));

        // Jerarquía: ADMIN_BUZON > RESPONDER > LECTOR
        int nivelTengo = nivel(acceso.getRol());
        int nivelRequerido = nivel(rolMinimo);
        if (nivelTengo < nivelRequerido)
            throw new InboxApiException(ServerApiStatusCode.ACCESO_DENEGADO,
                    "Tu rol " + acceso.getRol() + " no alcanza para " + rolMinimo);
    }

    private static int nivel(RolBuzon r) {
        return switch (r) {
            case LECTOR -> 1;
            case RESPONDER -> 2;
            case ADMIN_BUZON -> 3;
        };
    }

    private Mensaje getMensajeOrThrow(Long id) {
        return mensajeRepo.findById(id)
                .orElseThrow(() -> new InboxApiException(ServerApiStatusCode.MENSAJE_NO_ENCONTRADO));
    }

    private MensajeResponse toResponse(Mensaje m) {
        return MensajeResponse.builder()
                .id(m.getId())
                .buzonId(m.getBuzon().getId())
                .remitenteEmail(m.getRemitenteEmail())
                .remitenteNombre(m.getRemitenteNombre())
                .asunto(m.getAsunto())
                .snippet(m.getSnippet())
                .fechaRecibido(m.getFechaRecibido())
                .estado(m.getEstado())
                .carpeta(m.getCarpeta())
                .leido(m.getLeido())
                .destacado(m.getDestacado())
                .tieneAdjuntos(m.getTieneAdjuntos())
                .threadId(m.getThreadId())
                .build();
    }

    private static String serializarLista(List<String> lista) {
        try {
            return MAPPER.writeValueAsString(lista);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private static List<String> deserializarLista(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private static List<String> unirListas(List<String>... listas) {
        List<String> r = new ArrayList<>();
        for (var l : listas) if (l != null) r.addAll(l);
        return r;
    }

    private static String truncar(String s, int n) {
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String stripS3Prefix(String s3Key) {
        if (s3Key == null) return null;
        if (s3Key.startsWith("s3://")) {
            int slash = s3Key.indexOf('/', 5);
            return slash > 0 ? s3Key.substring(slash + 1) : s3Key;
        }
        return s3Key;
    }
}
