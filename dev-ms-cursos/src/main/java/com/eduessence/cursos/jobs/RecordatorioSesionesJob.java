package com.eduessence.cursos.jobs;

import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.feign.SendmailServiceClient;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.entity.SesionVirtual;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.repository.SesionVirtualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Job diario que envía {@code SUMMIT_RECORDATORIO} a los matriculados con
 * sesiones programadas para el día siguiente.
 *
 * <p>Se ejecuta cada mañana a las 08:00 (hora del servidor). Busca sesiones
 * no canceladas cuya {@code fechaInicio} caiga entre "mañana 00:00" y
 * "mañana 23:59:59", y por cada una notifica a los matriculados ACTIVA /
 * APROBADA / FINALIZADA del curso al que pertenece.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioSesionesJob {

    private static final DateTimeFormatter FMT_FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));
    private static final DateTimeFormatter FMT_HORA =
            DateTimeFormatter.ofPattern("HH:mm", new Locale("es", "CO"));

    private final SesionVirtualRepository sesionRepo;
    private final MatriculaRepository matriculaRepo;
    private final CursoRepository cursoRepo;
    private final AuthenticateServiceClient authClient;
    private final SendmailServiceClient sendmail;

    /** Cron diario a las 08:00 (configurable por env). */
    @Scheduled(cron = "${app.cursos.cron-recordatorio-sesiones:0 0 8 * * *}")
    public void recordatoriosDiarios() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.withHour(23).withMinute(59).withSecond(59);

        log.info("Ejecutando recordatorios para sesiones entre {} y {}", inicio, fin);

        List<SesionVirtual> sesionesManana = sesionRepo.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getCancelada()))
                .filter(s -> s.getFechaInicio() != null)
                .filter(s -> !s.getFechaInicio().isBefore(inicio) && !s.getFechaInicio().isAfter(fin))
                .toList();

        if (sesionesManana.isEmpty()) {
            log.info("No hay sesiones mañana — sin recordatorios");
            return;
        }

        int totalRecordatorios = 0;
        for (SesionVirtual sesion : sesionesManana) {
            totalRecordatorios += notificarSesion(sesion);
        }
        log.info("Recordatorios enviados: {} para {} sesiones",
                totalRecordatorios, sesionesManana.size());
    }

    private int notificarSesion(SesionVirtual sesion) {
        Curso curso = cursoRepo.findById(sesion.getCursoId()).orElse(null);
        if (curso == null) return 0;

        List<Matricula> matriculados = matriculaRepo.findAll().stream()
                .filter(m -> sesion.getCursoId().equals(m.getCurso().getId()))
                .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA
                          || m.getEstado() == EstadoMatricula.APROBADA
                          || m.getEstado() == EstadoMatricula.FINALIZADA)
                .toList();

        if (matriculados.isEmpty()) return 0;

        String fechaLarga = sesion.getFechaInicio().format(FMT_FECHA_LARGA);
        String hora = sesion.getFechaInicio().format(FMT_HORA);
        String fechaInicio = fechaLarga + " a las " + hora;
        String tituloSesion = sesion.getTitulo() == null || sesion.getTitulo().isBlank()
                ? curso.getNombre()
                : sesion.getTitulo();
        // urlStreaming = playback URL si es VIRTUAL, ubicación si es PRESENCIAL
        String urlStreaming;
        switch (sesion.getTipo()) {
            case VIRTUAL -> urlStreaming = sesion.getPlaybackUrl() == null
                    ? "" : sesion.getPlaybackUrl();
            default -> urlStreaming = sesion.getUbicacionTexto() == null
                    ? "" : sesion.getUbicacionTexto();
        }

        int enviados = 0;
        for (Matricula m : matriculados) {
            UsuarioDestinatario dest = resolverDestinatario(m.getUsuarioId());
            if (dest.correo == null || dest.correo.isBlank()) continue;
            try {
                sendmail.enviarEmail(Map.of(
                        "nombreTemplate", "SUMMIT_RECORDATORIO",
                        "destinatario", Map.of(
                                "correo", dest.correo,
                                "nombre", dest.nombre == null ? "" : dest.nombre),
                        "variables", Map.of(
                                "nombreDestinatario", dest.nombre == null ? "" : dest.nombre,
                                "nombreEvento", tituloSesion + " · " + curso.getNombre(),
                                "fechaInicio", fechaInicio,
                                "urlStreaming", urlStreaming
                        )
                ));
                enviados++;
            } catch (Exception ex) {
                log.warn("Recordatorio a matrícula {} falló: {}", m.getId(), ex.getMessage());
            }
        }
        log.info("Sesión {} ({}) → {} recordatorios enviados",
                sesion.getId(), sesion.getTitulo(), enviados);
        return enviados;
    }

    /* ─────────────── Lookup del email real ─────────────── */

    private record UsuarioDestinatario(String correo, String nombre) {}

    @SuppressWarnings("unchecked")
    private UsuarioDestinatario resolverDestinatario(Long usuarioId) {
        try {
            Map<String, Object> resp = authClient.lookup(List.of(usuarioId));
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> u) {
                Map<String, Object> mu = (Map<String, Object>) u;
                String email = mu.get("email") == null ? null : mu.get("email").toString();
                String nombres = mu.get("nombres") == null ? "" : mu.get("nombres").toString();
                String apellidos = mu.get("apellidos") == null ? "" : mu.get("apellidos").toString();
                String nombreCompleto = (nombres + " " + apellidos).trim();
                return new UsuarioDestinatario(email, nombreCompleto.isEmpty() ? nombres : nombreCompleto);
            }
        } catch (Exception ex) {
            log.warn("Lookup del usuario {} falló: {}", usuarioId, ex.getMessage());
        }
        return new UsuarioDestinatario(null, null);
    }
}
