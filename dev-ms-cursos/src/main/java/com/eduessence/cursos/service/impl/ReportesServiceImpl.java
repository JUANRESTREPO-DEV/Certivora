package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.feign.CertificadosServiceClient;
import com.eduessence.cursos.feign.PagosServiceClient;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaCursoResponse;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaDetalleResponse;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaDetalleResponse.AsistenteReporte;
import com.eduessence.cursos.model.dto.response.ReporteAsistenciaDetalleResponse.SesionResumen;
import com.eduessence.cursos.model.dto.response.ReporteCertificadosResponse;
import com.eduessence.cursos.model.dto.response.ReporteMatriculasResponse;
import com.eduessence.cursos.model.entity.AsistenciaPresencial;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.CursoModalidad;
import com.eduessence.cursos.model.entity.Matricula;
import com.eduessence.cursos.model.entity.SesionVirtual;
import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.model.enums.TipoSesion;
import com.eduessence.cursos.repository.AsistenciaPresencialRepository;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.MatriculaRepository;
import com.eduessence.cursos.repository.SesionVirtualRepository;
import com.eduessence.cursos.service.ReportesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportesServiceImpl implements ReportesService {

    private final CursoRepository cursoRepo;
    private final SesionVirtualRepository sesionRepo;
    private final MatriculaRepository matriculaRepo;
    private final AsistenciaPresencialRepository asistenciaRepo;
    private final PagosServiceClient pagosClient;
    private final CertificadosServiceClient certificadosClient;
    private final AuthenticateServiceClient authClient;

    /* ─────────────────────── Asistencia ─────────────────────── */

    @Override
    @Transactional(readOnly = true)
    public List<ReporteAsistenciaCursoResponse> asistenciaCursos() {
        // Cursos con al menos una sesión PRESENCIAL
        Map<Long, List<SesionVirtual>> sesionesPorCurso = new HashMap<>();
        for (SesionVirtual s : sesionRepo.findAll()) {
            if (s.getTipo() != TipoSesion.PRESENCIAL) continue;
            sesionesPorCurso.computeIfAbsent(s.getCursoId(), k -> new ArrayList<>()).add(s);
        }
        if (sesionesPorCurso.isEmpty()) return List.of();

        List<ReporteAsistenciaCursoResponse> out = new ArrayList<>();
        for (Map.Entry<Long, List<SesionVirtual>> e : sesionesPorCurso.entrySet()) {
            Long cursoId = e.getKey();
            Curso c = cursoRepo.findById(cursoId).orElse(null);
            if (c == null) continue;

            List<SesionVirtual> sesiones = e.getValue();
            long matriculados = matriculaRepo.findAllByCurso_Id(cursoId).stream()
                    .filter(this::tienePresencialActivo).count();
            long checkins = sesiones.stream()
                    .mapToLong(s -> asistenciaRepo.countBySesionVirtualId(s.getId()))
                    .sum();
            long posibles = matriculados * sesiones.size();
            double pct = posibles == 0 ? 0.0 : (checkins * 100.0 / posibles);

            String sede = c.getModalidades().stream()
                    .filter(cm -> Boolean.TRUE.equals(cm.getActivo())
                            && cm.getTipo() == ModalidadTipo.PRESENCIAL)
                    .map(CursoModalidad::getSede)
                    .filter(x -> x != null && !x.isBlank())
                    .findFirst().orElse(null);

            out.add(ReporteAsistenciaCursoResponse.builder()
                    .cursoId(c.getId())
                    .nombre(c.getNombre())
                    .slug(c.getSlug())
                    .sede(sede)
                    .fechaInicio(c.getFechaInicio())
                    .fechaFin(c.getFechaFin())
                    .totalSesiones(sesiones.size())
                    .totalMatriculados(matriculados)
                    .totalCheckins(checkins)
                    .checkinsPosibles(posibles)
                    .porcentajeAsistencia(pct)
                    .build());
        }

        out.sort(compareByFechaInicioDesc());
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteAsistenciaDetalleResponse asistenciaDetalle(Long cursoId) {
        Curso c = cursoRepo.findById(cursoId).orElse(null);
        if (c == null) return null;

        List<SesionVirtual> sesiones = sesionRepo
                .findAllByCursoIdOrderByFechaInicioAsc(cursoId).stream()
                .filter(s -> s.getTipo() == TipoSesion.PRESENCIAL)
                .toList();

        List<SesionResumen> sesionesResp = sesiones.stream()
                .map(s -> SesionResumen.builder()
                        .sesionId(s.getId())
                        .titulo(s.getTitulo())
                        .fechaInicio(s.getFechaInicio())
                        .duracionMinutos(s.getDuracionMinutos())
                        .cancelada(Boolean.TRUE.equals(s.getCancelada()))
                        .checkins(asistenciaRepo.countBySesionVirtualId(s.getId()))
                        .build())
                .toList();

        List<Matricula> matriculados = matriculaRepo.findAllByCurso_Id(cursoId).stream()
                .filter(this::tienePresencialActivo).toList();

        // Batch lookup usuarios
        List<Long> userIds = matriculados.stream().map(Matricula::getUsuarioId).toList();
        Map<Long, Map<String, Object>> usuarios = lookupUsuarios(userIds);

        // Todas las asistencias del curso (por sesión → matriculaId → fecha)
        Map<Long, Map<Long, LocalDateTime>> asistenciasPorMatricula = new HashMap<>();
        for (SesionVirtual s : sesiones) {
            for (AsistenciaPresencial a : asistenciaRepo.findAllBySesionVirtualId(s.getId())) {
                asistenciasPorMatricula
                        .computeIfAbsent(a.getMatriculaId(), k -> new HashMap<>())
                        .put(s.getId(), a.getFechaCheckin());
            }
        }

        int totalSes = sesiones.size();
        List<AsistenteReporte> asistentesResp = matriculados.stream().map(m -> {
            Map<String, Object> u = usuarios.getOrDefault(m.getUsuarioId(), Map.of());
            String nombre = (strOr(u, "nombres", "") + " " + strOr(u, "apellidos", "")).trim();
            Map<Long, LocalDateTime> checks = asistenciasPorMatricula
                    .getOrDefault(m.getId(), Map.of());
            int totalCheckins = checks.size();
            double pct = totalSes == 0 ? 0.0 : (totalCheckins * 100.0 / totalSes);
            return AsistenteReporte.builder()
                    .matriculaId(m.getId())
                    .usuarioId(m.getUsuarioId())
                    .nombreCompleto(nombre.isEmpty() ? "Asistente #" + m.getUsuarioId() : nombre)
                    .email(strOr(u, "email", ""))
                    .documento(strOr(u, "documento", ""))
                    .checkinsPorSesion(checks)
                    .totalCheckins(totalCheckins)
                    .porcentajeAsistencia(pct)
                    .build();
        })
        .sorted((a, b) -> a.getNombreCompleto().compareToIgnoreCase(b.getNombreCompleto()))
        .toList();

        String sede = c.getModalidades().stream()
                .filter(cm -> Boolean.TRUE.equals(cm.getActivo())
                        && cm.getTipo() == ModalidadTipo.PRESENCIAL)
                .map(CursoModalidad::getSede)
                .filter(x -> x != null && !x.isBlank())
                .findFirst().orElse(null);

        return ReporteAsistenciaDetalleResponse.builder()
                .cursoId(c.getId())
                .nombre(c.getNombre())
                .sede(sede)
                .sesiones(sesionesResp)
                .asistentes(asistentesResp)
                .build();
    }

    /* ─────────────────────── Matrículas + ingresos ─────────────────────── */

    @Override
    @Transactional(readOnly = true)
    public ReporteMatriculasResponse matriculas() {
        Map<Long, Long> ingresos = fetchIngresosPorCurso();

        List<ReporteMatriculasResponse.Fila> filas = new ArrayList<>();
        long totalM = 0, totalP = 0, totalV = 0, totalG = 0;
        BigDecimal totalIng = BigDecimal.ZERO;

        for (Curso c : cursoRepo.findAll()) {
            List<Matricula> ms = matriculaRepo.findAllByCurso_Id(c.getId()).stream()
                    .filter(m -> m.getEstado() != EstadoMatricula.CANCELADA
                            && m.getEstado() != EstadoMatricula.REEMBOLSADA)
                    .toList();
            if (ms.isEmpty()) continue;

            long p = ms.stream().filter(m -> tieneModalidad(m, ModalidadTipo.PRESENCIAL)).count();
            long v = ms.stream().filter(m -> tieneModalidad(m, ModalidadTipo.VIRTUAL_LIVE)).count();
            long g = ms.stream().filter(m -> tieneModalidad(m, ModalidadTipo.GRABADO)).count();

            BigDecimal ing = BigDecimal.valueOf(ingresos.getOrDefault(c.getId(), 0L));

            filas.add(ReporteMatriculasResponse.Fila.builder()
                    .cursoId(c.getId())
                    .nombre(c.getNombre())
                    .slug(c.getSlug())
                    .fechaInicio(c.getFechaInicio())
                    .matriculas(ms.size())
                    .presencial(p)
                    .virtualLive(v)
                    .grabado(g)
                    .ingresos(ing)
                    .estado(c.getEstado() == null ? "" : c.getEstado().name())
                    .build());

            totalM += ms.size();
            totalP += p;
            totalV += v;
            totalG += g;
            totalIng = totalIng.add(ing);
        }

        filas.sort(Comparator.comparing(
                ReporteMatriculasResponse.Fila::getFechaInicio,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return ReporteMatriculasResponse.builder()
                .filas(filas)
                .totalMatriculas(totalM)
                .totalPresencial(totalP)
                .totalVirtualLive(totalV)
                .totalGrabado(totalG)
                .ingresosTotales(totalIng)
                .build();
    }

    /* ─────────────────────── Certificados ─────────────────────── */

    @Override
    @Transactional(readOnly = true)
    public ReporteCertificadosResponse certificados() {
        Map<Long, Long> emitidosPorCurso = fetchEmitidosPorCurso();

        List<ReporteCertificadosResponse.Fila> filas = new ArrayList<>();
        long totalEmit = 0, totalPend = 0;

        for (Curso c : cursoRepo.findAll()) {
            if (!Boolean.TRUE.equals(c.getEmiteCertificado())) continue;

            long matriculados = matriculaRepo.findAllByCurso_Id(c.getId()).stream()
                    .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA
                            || m.getEstado() == EstadoMatricula.APROBADA
                            || m.getEstado() == EstadoMatricula.FINALIZADA)
                    .count();

            long emitidos = emitidosPorCurso.getOrDefault(c.getId(), 0L);
            long pendientes = Math.max(0, matriculados - emitidos);
            double pct = matriculados == 0 ? 0.0 : (emitidos * 100.0 / matriculados);

            filas.add(ReporteCertificadosResponse.Fila.builder()
                    .cursoId(c.getId())
                    .nombre(c.getNombre())
                    .slug(c.getSlug())
                    .fechaFin(c.getFechaFin())
                    .totalMatriculados(matriculados)
                    .emitidos(emitidos)
                    .pendientes(pendientes)
                    .porcentajeEmision(pct)
                    .build());

            totalEmit += emitidos;
            totalPend += pendientes;
        }

        filas.sort(Comparator.comparing(
                ReporteCertificadosResponse.Fila::getFechaFin,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return ReporteCertificadosResponse.builder()
                .filas(filas)
                .totalEmitidos(totalEmit)
                .totalPendientes(totalPend)
                .build();
    }

    /* ─────────────────────── Helpers ─────────────────────── */

    private boolean tienePresencialActivo(Matricula m) {
        if (m.getModalidades() == null || !m.getModalidades().contains(ModalidadTipo.PRESENCIAL))
            return false;
        EstadoMatricula e = m.getEstado();
        return e == EstadoMatricula.ACTIVA
                || e == EstadoMatricula.APROBADA
                || e == EstadoMatricula.FINALIZADA;
    }

    private boolean tieneModalidad(Matricula m, ModalidadTipo t) {
        return m.getModalidades() != null && m.getModalidades().contains(t);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> fetchIngresosPorCurso() {
        try {
            Map<String, Object> resp = pagosClient.ingresosPorCurso();
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof Map<?, ?> m) {
                Map<Long, Long> out = new HashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    Long id = parseLong(e.getKey());
                    Long v  = parseLong(e.getValue());
                    if (id != null && v != null) out.put(id, v);
                }
                return out;
            }
        } catch (Exception ex) {
            log.warn("[Reportes] No pudimos leer ingresos por curso: {}", ex.getMessage());
        }
        return Map.of();
    }

    private Map<Long, Long> fetchEmitidosPorCurso() {
        try {
            Map<String, Object> resp = certificadosClient.emitidosPorCurso();
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof Map<?, ?> m) {
                Map<Long, Long> out = new HashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    Long id = parseLong(e.getKey());
                    Long v  = parseLong(e.getValue());
                    if (id != null && v != null) out.put(id, v);
                }
                return out;
            }
        } catch (Exception ex) {
            log.warn("[Reportes] No pudimos leer certificados emitidos: {}", ex.getMessage());
        }
        return Map.of();
    }

    private static Long parseLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, Object>> lookupUsuarios(List<Long> ids) {
        Map<Long, Map<String, Object>> out = new HashMap<>();
        if (ids == null || ids.isEmpty()) return out;
        try {
            Map<String, Object> resp = authClient.lookup(ids);
            Object dataObj = resp == null ? null : resp.get("response");
            if (dataObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> mu) {
                        Map<String, Object> m = (Map<String, Object>) mu;
                        Object idObj = m.get("id");
                        if (idObj instanceof Number n) out.put(n.longValue(), m);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[Reportes] Lookup batch usuarios falló: {}", ex.getMessage());
        }
        return out;
    }

    private static Comparator<ReporteAsistenciaCursoResponse> compareByFechaInicioDesc() {
        return (a, b) -> {
            if (a.getFechaInicio() == null && b.getFechaInicio() == null) return 0;
            if (a.getFechaInicio() == null) return 1;
            if (b.getFechaInicio() == null) return -1;
            return b.getFechaInicio().compareTo(a.getFechaInicio());
        };
    }

    private static String strOr(Map<String, Object> m, String k, String def) {
        if (m == null || m.get(k) == null) return def;
        return m.get(k).toString();
    }
}
