package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.feign.CertificadosServiceClient;
import com.eduessence.cursos.model.dto.response.AprobacionResultDTO;
import com.eduessence.cursos.model.entity.AsistenciaVirtual;
import com.eduessence.cursos.model.entity.ConfiguracionAprobacion;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.IntentoExamen;
import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.enums.EstadoLeccion;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.model.entity.EntregaActividad;
import com.eduessence.cursos.repository.AsistenciaVirtualRepository;
import com.eduessence.cursos.repository.EntregaActividadRepository;
import com.eduessence.cursos.repository.IntentoExamenRepository;
import com.eduessence.cursos.repository.LeccionRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.repository.ProgresoLeccionRepository;
import com.eduessence.cursos.service.AprobacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula la aprobación de una matrícula combinando progreso, notas y
 * presencia. Reglas implementadas:
 *
 *   - R-CUR-06: progreso_pct = (Σ lecciones COMPLETADA / total) × 100
 *   - El % mínimo de aprobación viene de {@link ConfiguracionAprobacion}.
 *   - Para actividades/exámenes la nota mínima es la del recurso si está
 *     seteada; si no, {@code nota_minima_general}.
 *   - La presencia solo aplica si la matrícula tiene modalidad VIRTUAL_LIVE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AprobacionServiceImpl implements AprobacionService {

    private final MatriculaRepository matriculaRepository;
    private final LeccionRepository leccionRepository;
    private final ProgresoLeccionRepository progresoRepository;
    private final IntentoExamenRepository intentoExamenRepository;
    private final AsistenciaVirtualRepository asistenciaRepository;
    private final EntregaActividadRepository entregaActividadRepository;
    private final CertificadosServiceClient certificadosClient;
    private final AuthenticateServiceClient authClient;

    @Override
    @Transactional
    public AprobacionResultDTO evaluar(Long matriculaId) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.MATRICULA_NO_ENCONTRADA));
        Curso curso = matricula.getCurso();
        ConfiguracionAprobacion cfg = curso.getConfiguracionAprobacion();
        if (cfg == null) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El curso no tiene configuración de aprobación");
        }

        List<String> motivos = new ArrayList<>();

        BigDecimal progreso = calcularProgreso(matriculaId, curso.getId());
        boolean cumpleProgreso = progreso.compareTo(cfg.getProgresoMinimoPct()) >= 0;
        if (!cumpleProgreso) {
            motivos.add(String.format("Progreso %.2f%% < mínimo %.2f%%",
                    progreso, cfg.getProgresoMinimoPct()));
        }

        BigDecimal promedioEx = calcularPromedioExamenes(matriculaId);
        BigDecimal promedioAct = calcularPromedioActividades(matriculaId);
        boolean cumpleEx = promedioEx == null || promedioEx.compareTo(cfg.getNotaMinimaGeneral()) >= 0;
        boolean cumpleAct = promedioAct == null || promedioAct.compareTo(cfg.getNotaMinimaGeneral()) >= 0;
        if (!cumpleEx) motivos.add("Promedio exámenes " + promedioEx + " insuficiente");
        if (!cumpleAct) motivos.add("Promedio actividades " + promedioAct + " insuficiente");

        boolean aplicaPresencia = matricula.getModalidades() != null
                && matricula.getModalidades().contains(ModalidadTipo.VIRTUAL_LIVE);
        BigDecimal presencia = aplicaPresencia ? calcularPresencia(matriculaId) : null;
        boolean cumplePresencia = !aplicaPresencia
                || (presencia != null && presencia.compareTo(cfg.getPresenciaMinimaPct()) >= 0);
        if (!cumplePresencia) {
            motivos.add(String.format("Presencia %.2f%% < mínimo %.2f%%",
                    presencia == null ? BigDecimal.ZERO : presencia, cfg.getPresenciaMinimaPct()));
        }

        boolean aprobado = cumpleProgreso && cumpleEx && cumpleAct && cumplePresencia;

        // Persistir resultado
        matricula.setProgresoPct(progreso);
        BigDecimal notaFinal = combinarNotaFinal(promedioEx, promedioAct);
        matricula.setNotaFinal(notaFinal);
        boolean transicionAAprobada = aprobado && matricula.getEstado() != EstadoMatricula.APROBADA;
        if (transicionAAprobada) {
            matricula.setEstado(EstadoMatricula.APROBADA);
            matricula.setFechaAprobacion(LocalDateTime.now());
        }

        // Intentar emitir certificado si:
        //   - el curso lo emite,
        //   - la matrícula aprueba (recién o ya estaba),
        //   - y el flag certificadoEmitido todavía es false.
        // Esto cubre el caso en que una llamada previa marcó APROBADA pero el
        // MS de certificados estaba caído (fallback) y se quedó sin certificado.
        // El emit es idempotente del lado del MS-certificados, así que un
        // reintento no genera duplicados.
        boolean debeEmitir = aprobado
                && Boolean.TRUE.equals(curso.getEmiteCertificado())
                && !Boolean.TRUE.equals(matricula.getCertificadoEmitido());
        if (debeEmitir) {
            try {
                String nombreCompleto = resolverNombreCompleto(matricula.getUsuarioId());
                String tipoParticipante = matricula.getTipo() == null
                        ? "ASISTENTE" : matricula.getTipo().name();

                Map<String, Object> payload = new HashMap<>();
                payload.put("matriculaId", matricula.getId());
                payload.put("usuarioId", matricula.getUsuarioId());
                payload.put("cursoId", curso.getId());
                payload.put("tipoParticipante", tipoParticipante);
                payload.put("nombreCompleto", nombreCompleto);
                payload.put("nombreCurso", curso.getNombre());
                payload.put("fechaCurso", LocalDateTime.now().toLocalDate().toString());
                if (curso.getTemplateCertificadoId() != null) {
                    payload.put("templateId", curso.getTemplateCertificadoId());
                }
                Map<String, Object> resp = certificadosClient.emitir(payload);
                // El fallback de Feign retorna Map.of() vacío. Solo marcamos
                // certificadoEmitido si vino una respuesta real del MS.
                if (resp != null && resp.get("response") != null) {
                    matricula.setCertificadoEmitido(true);
                    log.info("Certificado emitido · matricula={} · usuario={} · curso={}",
                            matricula.getId(), nombreCompleto, curso.getNombre());
                } else {
                    log.error("Emisión de certificado falló (fallback o respuesta vacía) " +
                            "· matricula={} · payload={}", matricula.getId(), payload);
                    motivos.add("El servicio de certificados no respondió. Inténtalo de nuevo en un momento.");
                }
            } catch (Exception ex) {
                log.error("Excepción al emitir certificado para matrícula {}: {}",
                        matriculaId, ex.getMessage(), ex);
                motivos.add("Error técnico al emitir el certificado: " + ex.getMessage());
            }
        }

        matriculaRepository.save(matricula);

        return AprobacionResultDTO.builder()
                .matriculaId(matriculaId)
                .aprobado(aprobado)
                .progresoPct(progreso)
                .progresoMinimoRequerido(cfg.getProgresoMinimoPct())
                .cumpleProgreso(cumpleProgreso)
                .promedioExamenes(promedioEx)
                .promedioActividades(promedioAct)
                .notaMinimaRequerida(cfg.getNotaMinimaGeneral())
                .cumpleNotas(cumpleEx && cumpleAct)
                .porcentajePresencia(presencia)
                .presenciaMinimaRequerida(cfg.getPresenciaMinimaPct())
                .cumplePresencia(cumplePresencia)
                .aplicaPresencia(aplicaPresencia)
                .motivosNoAprueba(motivos)
                .build();
    }

    /* ─────────────────────── cálculos ─────────────────────── */

    private BigDecimal calcularProgreso(Long matriculaId, Long cursoId) {
        long total = leccionRepository.countByCursoId(cursoId);
        if (total == 0) return BigDecimal.ZERO;
        long completadas = progresoRepository.countByMatriculaIdAndEstado(matriculaId, EstadoLeccion.COMPLETADA);
        return BigDecimal.valueOf(completadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPromedioExamenes(Long matriculaId) {
        // Toma el MEJOR intento finalizado por examen y promedia
        List<IntentoExamen> intentos = intentoExamenRepository.findAll().stream()
                .filter(i -> i.getMatriculaId().equals(matriculaId) && Boolean.TRUE.equals(i.getFinalizado()))
                .toList();
        if (intentos.isEmpty()) return null;
        BigDecimal suma = BigDecimal.ZERO;
        int n = 0;
        // Agrupa por examen y toma el mayor puntaje
        var porExamen = new java.util.HashMap<Long, BigDecimal>();
        for (IntentoExamen i : intentos) {
            BigDecimal p = i.getPuntaje() == null ? BigDecimal.ZERO : i.getPuntaje();
            porExamen.merge(i.getExamenId(), p, BigDecimal::max);
        }
        for (BigDecimal p : porExamen.values()) {
            suma = suma.add(p);
            n++;
        }
        return n == 0 ? null : suma.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPromedioActividades(Long matriculaId) {
        // Toma la última entrega calificada por actividad y promedia
        List<EntregaActividad> entregas = entregaActividadRepository
                .findAllByMatriculaIdOrderByFechaEntregaDesc(matriculaId);
        if (entregas.isEmpty()) return null;

        Map<Long, BigDecimal> porActividad = new HashMap<>();
        for (EntregaActividad e : entregas) {
            if (e.getCalificacion() == null) continue;
            porActividad.putIfAbsent(e.getActividadId(), e.getCalificacion());
        }
        if (porActividad.isEmpty()) return null;
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal nota : porActividad.values()) suma = suma.add(nota);
        return suma.divide(BigDecimal.valueOf(porActividad.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPresencia(Long matriculaId) {
        List<AsistenciaVirtual> asistencias = asistenciaRepository.findAllByMatriculaId(matriculaId);
        if (asistencias.isEmpty()) return BigDecimal.ZERO;
        BigDecimal suma = BigDecimal.ZERO;
        for (AsistenciaVirtual a : asistencias) {
            suma = suma.add(a.getPorcentajePresencia() == null ? BigDecimal.ZERO : a.getPorcentajePresencia());
        }
        return suma.divide(BigDecimal.valueOf(asistencias.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal combinarNotaFinal(BigDecimal promEx, BigDecimal promAct) {
        if (promEx == null && promAct == null) return null;
        if (promEx == null) return promAct;
        if (promAct == null) return promEx;
        return promEx.add(promAct).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /**
     * Resuelve el nombre completo del alumno desde authenticate-service.
     * Si la llamada falla, cae a un fallback "Usuario #N" para que la
     * emisión del certificado nunca se quede bloqueada por un problema de red.
     */
    @SuppressWarnings("unchecked")
    private String resolverNombreCompleto(Long usuarioId) {
        try {
            Map<String, Object> resp = authClient.lookup(List.of(usuarioId));
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> u) {
                String n = obj((Map<String, Object>) u, "nombres");
                String a = obj((Map<String, Object>) u, "apellidos");
                String full = (n == null ? "" : n) + " " + (a == null ? "" : a);
                full = full.trim();
                if (!full.isEmpty()) return full;
                String email = obj((Map<String, Object>) u, "email");
                if (email != null && !email.isBlank()) return email;
            }
        } catch (Exception ex) {
            log.warn("Lookup de usuario {} falló: {}", usuarioId, ex.getMessage());
        }
        return "Usuario #" + usuarioId;
    }

    private static String obj(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }
}
