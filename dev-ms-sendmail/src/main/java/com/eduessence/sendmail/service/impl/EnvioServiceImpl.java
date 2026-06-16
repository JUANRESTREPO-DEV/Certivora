package com.eduessence.sendmail.service.impl;

import com.eduessence.sendmail.exception.SendmailApiException;
import com.eduessence.sendmail.exception.ServerApiStatusCode;
import com.eduessence.sendmail.model.dto.DestinatarioDTO;
import com.eduessence.sendmail.model.dto.EmailEnvioResponse;
import com.eduessence.sendmail.model.dto.PageResponse;
import com.eduessence.sendmail.model.entity.EmailEnvio;
import com.eduessence.sendmail.model.entity.EmailTemplate;
import com.eduessence.sendmail.model.enums.EstadoEnvio;
import com.eduessence.sendmail.repository.EmailEnvioRepository;
import com.eduessence.sendmail.service.EmailLogService;
import com.eduessence.sendmail.service.EmailRenderer;
import com.eduessence.sendmail.service.EnvioService;
import com.eduessence.sendmail.service.S3TemplateLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvioServiceImpl implements EnvioService {

    private static final int MAX_PAGE_SIZE = 200;

    private final EmailEnvioRepository envioRepository;
    private final S3TemplateLoader s3Loader;
    private final EmailRenderer renderer;
    private final EmailLogService logService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmailEnvioResponse> buscar(EstadoEnvio estado,
                                                   Long templateId,
                                                   LocalDateTime desde,
                                                   LocalDateTime hasta,
                                                   String q,
                                                   int page,
                                                   int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<EmailEnvio> pg = envioRepository.buscar(estado, templateId, desde, hasta, q, pageable);

        List<EmailEnvioResponse> content = pg.getContent().stream()
                .map(this::toResponse).toList();
        return PageResponse.<EmailEnvioResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .first(pg.isFirst())
                .last(pg.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmailEnvioResponse obtener(Long id) {
        EmailEnvio e = envioRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Envío no encontrado"));
        return toResponse(e);
    }

    @Override
    @Transactional
    public EmailEnvioResponse reintentar(Long id) {
        EmailEnvio original = envioRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Envío no encontrado"));
        EmailTemplate template = original.getTemplate();
        if (template == null) {
            throw new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO,
                    "El envío original no tiene template asociado y no puede reintentarse");
        }

        // Reconstruir contexto desde variables_usadas. Si está vacío, usamos
        // un map vacío — los defaults del template + sistema cubren el resto.
        Map<String, Object> variables = parsearVariables(original.getVariablesUsadas());

        DestinatarioDTO destinatario = DestinatarioDTO.builder()
                .correo(original.getDestinatario())
                .nombre(original.getNombreDestinatario())
                .build();

        Map<String, Object> contexto = renderer.construirContexto(template, destinatario, variables);
        renderer.validarRequeridas(template, contexto);

        String html = s3Loader.descargar(template.getBucket(), template.getPathTemplate());
        String asunto = original.getAsunto() == null ? template.getAsuntoDefault() : original.getAsunto();
        String htmlRender = renderer.render(html, contexto);

        EmailEnvio nuevo;
        try {
            renderer.enviarMime(original.getDestinatario(), asunto, htmlRender,
                    (String) contexto.get("logoContentId"), null);
            logService.registrar(template, original.getDestinatario(),
                    original.getNombreDestinatario(), asunto,
                    contexto, EstadoEnvio.ENVIADO, null);
            log.info("Envío {} reintentado con éxito", id);
        } catch (Exception ex) {
            log.error("Reintento {} falló: {}", id, ex.getMessage(), ex);
            logService.registrar(template, original.getDestinatario(),
                    original.getNombreDestinatario(), asunto,
                    contexto, EstadoEnvio.FALLIDO, ex.getMessage());
            throw new SendmailApiException(ServerApiStatusCode.ERROR_SMTP, ex);
        }

        // Devolvemos el último registro de esta cadena (el más reciente para
        // este destinatario+template) para que el front lo muestre.
        nuevo = envioRepository.findAllByOrderByFechaEnvioDesc(PageRequest.of(0, 1))
                .stream().findFirst().orElse(original);
        return toResponse(nuevo);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> resumen() {
        Map<String, Long> r = new LinkedHashMap<>();
        r.put("ENVIADO", envioRepository.countByEstado(EstadoEnvio.ENVIADO));
        r.put("FALLIDO", envioRepository.countByEstado(EstadoEnvio.FALLIDO));
        r.put("REINTENTAR", envioRepository.countByEstado(EstadoEnvio.REINTENTAR));
        long total = r.values().stream().mapToLong(Long::longValue).sum();
        r.put("TOTAL", total);
        return r;
    }

    /* ─── helpers ─── */

    private Map<String, Object> parsearVariables(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (Exception ex) {
            log.warn("No se pudo parsear variables_usadas: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private EmailEnvioResponse toResponse(EmailEnvio e) {
        return EmailEnvioResponse.builder()
                .id(e.getId())
                .templateId(e.getTemplate() != null ? e.getTemplate().getId() : null)
                .templateNombre(e.getTemplate() != null ? e.getTemplate().getNombre() : null)
                .destinatario(e.getDestinatario())
                .nombreDestinatario(e.getNombreDestinatario())
                .asunto(e.getAsunto())
                .estado(e.getEstado())
                .variables(parsearVariables(e.getVariablesUsadas()))
                .mensajeError(e.getMensajeError())
                .fechaEnvio(e.getFechaEnvio())
                .build();
    }
}
