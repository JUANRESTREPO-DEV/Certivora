package com.eduessence.sendmail.service.impl;

import com.eduessence.sendmail.exception.SendmailApiException;
import com.eduessence.sendmail.exception.ServerApiStatusCode;
import com.eduessence.sendmail.model.dto.PreviewRequest;
import com.eduessence.sendmail.model.dto.PreviewResponse;
import com.eduessence.sendmail.model.dto.TemplateRequest;
import com.eduessence.sendmail.model.dto.TemplateResponse;
import com.eduessence.sendmail.model.dto.TemplateVariableDTO;
import com.eduessence.sendmail.model.dto.TestSendRequest;
import com.eduessence.sendmail.model.entity.EmailTemplate;
import com.eduessence.sendmail.model.entity.EmailTemplateVariable;
import com.eduessence.sendmail.model.enums.EstadoEnvio;
import com.eduessence.sendmail.repository.EmailTemplateRepository;
import com.eduessence.sendmail.service.EmailLogService;
import com.eduessence.sendmail.service.EmailRenderer;
import com.eduessence.sendmail.service.S3TemplateLoader;
import com.eduessence.sendmail.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final EmailTemplateRepository templateRepository;
    private final S3TemplateLoader s3Loader;
    private final EmailRenderer renderer;
    private final EmailLogService logService;

    @Override
    @Transactional(readOnly = true)
    public EmailTemplate findActivoPorNombre(String nombre) {
        return templateRepository.findActivoConVariables(nombre)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO,
                        "Template '" + nombre + "' no existe o está inactivo"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> listarActivos() {
        return templateRepository.findAllByActivoTrueOrderByNombreAsc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> listarTodos() {
        return templateRepository.findAllByOrderByNombreAsc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse obtener(Long id) {
        return toResponse(templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO)));
    }

    @Override
    @Transactional
    public TemplateResponse crear(TemplateRequest req) {
        if (templateRepository.findByNombreIgnoreCase(req.getNombre()).isPresent()) {
            throw new SendmailApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "Ya existe un template con nombre " + req.getNombre());
        }
        EmailTemplate t = EmailTemplate.builder()
                .nombre(req.getNombre().trim().toUpperCase())
                .pathTemplate(req.getPathTemplate().trim())
                .bucket(req.getBucket())
                .asuntoDefault(req.getAsuntoDefault())
                .descripcion(req.getDescripcion())
                .activo(req.getActivo() == null ? true : req.getActivo())
                .build();
        aplicarVariables(t, req.getVariables());
        return toResponse(templateRepository.save(t));
    }

    @Override
    @Transactional
    public TemplateResponse actualizar(Long id, TemplateRequest req) {
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));
        t.setPathTemplate(req.getPathTemplate().trim());
        t.setBucket(req.getBucket());
        t.setAsuntoDefault(req.getAsuntoDefault());
        t.setDescripcion(req.getDescripcion());
        if (req.getActivo() != null) t.setActivo(req.getActivo());
        t.getVariables().clear();
        aplicarVariables(t, req.getVariables());
        return toResponse(templateRepository.save(t));
    }

    @Override
    @Transactional
    public TemplateResponse cambiarEstado(Long id, boolean activo) {
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));
        t.setActivo(activo);
        return toResponse(templateRepository.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerHtml(Long id) {
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));
        return s3Loader.descargar(t.getBucket(), t.getPathTemplate());
    }

    @Override
    @Transactional
    public void guardarHtml(Long id, String html) {
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));
        s3Loader.subir(t.getBucket(), t.getPathTemplate(), html == null ? "" : html);
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResponse previsualizar(Long id, PreviewRequest req) {
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));

        // El admin puede pasar HTML editado en vivo o pedir el de S3 actual
        String html = req != null && req.getHtmlOverride() != null && !req.getHtmlOverride().isBlank()
                ? req.getHtmlOverride()
                : s3Loader.descargar(t.getBucket(), t.getPathTemplate());

        Map<String, Object> contexto = renderer.construirContexto(t,
                req == null ? null : req.getDestinatario(),
                req == null || req.getVariables() == null ? Map.of() : req.getVariables());

        // No lanza — devolvemos faltantes para que la UI los muestre
        List<String> faltantes = renderer.variablesFaltantes(t, contexto);
        String htmlRender = renderer.render(html, contexto);

        String asunto = req != null && req.getAsunto() != null && !req.getAsunto().isBlank()
                ? req.getAsunto() : t.getAsuntoDefault();

        return PreviewResponse.builder()
                .htmlRender(htmlRender)
                .asunto(asunto)
                .contexto(contexto)
                .variablesFaltantes(faltantes)
                .build();
    }

    @Override
    @Transactional
    public String enviarPrueba(Long id, TestSendRequest req) {
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new SendmailApiException(ServerApiStatusCode.TEMPLATE_NO_ENCONTRADO));
        if (req == null || req.getDestinatario() == null
                || req.getDestinatario().getCorreo() == null
                || req.getDestinatario().getCorreo().isBlank()) {
            throw new SendmailApiException(ServerApiStatusCode.DESTINATARIOS_VACIOS);
        }

        String html = req.getHtmlOverride() != null && !req.getHtmlOverride().isBlank()
                ? req.getHtmlOverride()
                : s3Loader.descargar(t.getBucket(), t.getPathTemplate());

        Map<String, Object> contexto = renderer.construirContexto(t,
                req.getDestinatario(),
                req.getVariables() == null ? Map.of() : req.getVariables());
        renderer.validarRequeridas(t, contexto);

        String asunto = req.getAsunto() != null && !req.getAsunto().isBlank()
                ? "[PRUEBA] " + req.getAsunto()
                : "[PRUEBA] " + t.getAsuntoDefault();
        String htmlRender = renderer.render(html, contexto);
        String correo = req.getDestinatario().getCorreo().trim();

        try {
            renderer.enviarMime(correo, asunto, htmlRender,
                    (String) contexto.get("logoContentId"), null);
            logService.registrar(t, correo, req.getDestinatario().getNombre(), asunto,
                    contexto, EstadoEnvio.ENVIADO, null);
            return "Correo de prueba enviado a " + correo;
        } catch (Exception ex) {
            log.error("Error enviando prueba a {}: {}", correo, ex.getMessage(), ex);
            logService.registrar(t, correo, req.getDestinatario().getNombre(), asunto,
                    contexto, EstadoEnvio.FALLIDO, ex.getMessage());
            throw new SendmailApiException(ServerApiStatusCode.ERROR_SMTP, ex);
        }
    }

    private void aplicarVariables(EmailTemplate t, List<TemplateVariableDTO> vars) {
        if (vars == null) return;
        for (TemplateVariableDTO v : vars) {
            t.getVariables().add(EmailTemplateVariable.builder()
                    .template(t)
                    .nombre(v.getNombre())
                    .tipoDato(v.getTipoDato() == null ? "STRING" : v.getTipoDato())
                    .requerido(v.getRequerido() == null ? Boolean.TRUE : v.getRequerido())
                    .valorDefecto(v.getValorDefecto())
                    .descripcion(v.getDescripcion())
                    .build());
        }
    }

    private TemplateResponse toResponse(EmailTemplate t) {
        List<TemplateVariableDTO> vars = new ArrayList<>();
        for (EmailTemplateVariable v : t.getVariables()) {
            vars.add(TemplateVariableDTO.builder()
                    .id(v.getId()).nombre(v.getNombre()).tipoDato(v.getTipoDato())
                    .requerido(v.getRequerido()).valorDefecto(v.getValorDefecto())
                    .descripcion(v.getDescripcion())
                    .build());
        }
        return TemplateResponse.builder()
                .id(t.getId()).nombre(t.getNombre()).pathTemplate(t.getPathTemplate())
                .bucket(t.getBucket()).asuntoDefault(t.getAsuntoDefault())
                .descripcion(t.getDescripcion()).activo(t.getActivo())
                .fechaCreacion(t.getFechaCreacion()).fechaActualizacion(t.getFechaActualizacion())
                .variables(vars)
                .build();
    }
}
