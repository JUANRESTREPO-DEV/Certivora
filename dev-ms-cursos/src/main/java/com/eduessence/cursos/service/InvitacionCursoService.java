package com.eduessence.cursos.service;

import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.feign.SendmailServiceClient;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.CursoModalidad;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de broadcast de invitaciones cuando se activa un curso.
 *
 * <p>Se dispara desde {@link com.eduessence.cursos.service.CursoService#activar}
 * de forma <b>asíncrona</b> (thread pool "broadcastExecutor"). Itera las
 * páginas de usuarios activos que expone dev-ms-authenticate y envía el
 * template {@code CURSO_NUEVO_LANZADO} en lotes pequeños para no saturar
 * SMTP.</p>
 *
 * <p>Idempotente: si el curso vuelve a activarse (ej. lo pasan a BORRADOR y
 * de nuevo a ACTIVO) se re-envía. Si no se desea, el caller controla.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitacionCursoService {

    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));

    private final CursoRepository cursoRepository;
    private final AuthenticateServiceClient authClient;
    private final SendmailServiceClient sendmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.broadcast.tam-lote:200}")
    private int tamLote;

    @Value("${app.broadcast.delay-ms:500}")
    private long delayEntreLotesMs;

    /**
     * Envía la invitación a todos los usuarios activos. NO lanza excepción —
     * los fallos por destinatario o por página se registran y se sigue.
     */
    @Async("broadcastExecutor")
    public void broadcastActivacion(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId).orElse(null);
        if (curso == null) {
            log.warn("[broadcast] curso {} no existe, se ignora", cursoId);
            return;
        }

        Map<String, String> extras = construirVariablesFijas(curso);
        int page = 0;
        int enviados = 0;
        int fallidos = 0;
        int totalPages = Integer.MAX_VALUE;
        long inicio = System.currentTimeMillis();
        log.info("[broadcast] iniciando invitación curso={} (\"{}\")", cursoId, curso.getNombre());

        while (page < totalPages) {
            Map<String, Object> pageResp = authClient.listarActivos(page, tamLote);
            Object dataObj = pageResp == null ? null : pageResp.get("response");
            if (!(dataObj instanceof Map<?, ?> data)) {
                log.warn("[broadcast] auth-service devolvió payload inesperado en pg={}", page);
                break;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> pageData = (Map<String, Object>) data;
            totalPages = intOrDefault(pageData.get("totalPages"), 0);

            Object itemsObj = pageData.get("items");
            if (!(itemsObj instanceof List<?> items) || items.isEmpty()) break;

            for (Object it : items) {
                if (!(it instanceof Map<?, ?> raw)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> u = (Map<String, Object>) raw;
                String email = strOr(u.get("email"), null);
                if (email == null || email.isBlank()) { fallidos++; continue; }
                String nombres = strOr(u.get("nombres"), "");
                String apellidos = strOr(u.get("apellidos"), "");
                String nombre = (nombres + " " + apellidos).trim();
                if (nombre.isEmpty()) nombre = nombres;

                try {
                    sendmail.enviarEmail(Map.of(
                            "nombreTemplate", "CURSO_NUEVO_LANZADO",
                            "destinatario", Map.of("correo", email, "nombre", nombre),
                            "variables", Map.of(
                                    "nombreDestinatario", nombre,
                                    "nombreCurso", extras.get("nombreCurso"),
                                    "descripcionCorta", extras.get("descripcionCorta"),
                                    "fechaInicio", extras.get("fechaInicio"),
                                    "modalidades", extras.get("modalidades"),
                                    "precioDesde", extras.get("precioDesde"),
                                    "urlDetalle", extras.get("urlDetalle")
                            )
                    ));
                    enviados++;
                } catch (Exception ex) {
                    fallidos++;
                    log.warn("[broadcast] falló envío a {}: {}", email, ex.getMessage());
                }
            }
            page++;
            if (page < totalPages && delayEntreLotesMs > 0) {
                try { Thread.sleep(delayEntreLotesMs); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }

        long ms = System.currentTimeMillis() - inicio;
        log.info("[broadcast] curso={} completado — enviados={} fallidos={} en {}ms",
                cursoId, enviados, fallidos, ms);
    }

    /* ─────────── helpers ─────────── */

    private Map<String, String> construirVariablesFijas(Curso c) {
        String fechaInicio = c.getFechaInicio() == null
                ? "Próximamente"
                : c.getFechaInicio().format(FMT_FECHA);

        // Modalidades legibles con precio
        String mods = c.getModalidades().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .sorted((a, b) -> a.getTipo().name().compareTo(b.getTipo().name()))
                .map(this::etiquetaModalidad)
                .collect(Collectors.joining(" · "));

        // "Desde $150.000 COP"
        BigDecimal min = c.getModalidades().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(CursoModalidad::getPrecioCop)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
        String precioDesde = min == null
                ? "Consultar"
                : min.signum() == 0 ? "Gratis" : formatCop(min);

        String urlDetalle = frontendUrl.replaceAll("/+$", "") + "/cursos/" + c.getSlug();

        return Map.of(
                "nombreCurso", n(c.getNombre()),
                "descripcionCorta", n(c.getDescripcionCorta()),
                "fechaInicio", fechaInicio,
                "modalidades", mods,
                "precioDesde", precioDesde,
                "urlDetalle", urlDetalle
        );
    }

    private String etiquetaModalidad(CursoModalidad cm) {
        String label = switch (cm.getTipo()) {
            case PRESENCIAL -> "Presencial";
            case VIRTUAL_LIVE -> "Virtual en vivo";
            case GRABADO -> "Grabado";
        };
        if (cm.getPrecioCop() == null) {
            return cm.getTipo() == ModalidadTipo.GRABADO ? label + " (bonus)" : label;
        }
        if (cm.getPrecioCop().signum() == 0) return label + " (gratis)";
        return label + " " + formatCop(cm.getPrecioCop());
    }

    private static String formatCop(BigDecimal v) {
        return "$" + String.format(Locale.forLanguageTag("es-CO"), "%,d", v.intValue()) + " COP";
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String strOr(Object o, String def) { return o == null ? def : o.toString(); }
    private static int intOrDefault(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }
}
