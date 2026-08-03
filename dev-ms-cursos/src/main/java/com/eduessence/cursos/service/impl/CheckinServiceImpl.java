package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.feign.AuthenticateServiceClient;
import com.eduessence.cursos.model.dto.request.CheckinScanRequest;
import com.eduessence.cursos.model.dto.response.AsistenteCheckinResponse;
import com.eduessence.cursos.model.dto.response.CheckinItemResponse;
import com.eduessence.cursos.model.dto.response.CheckinScanResponse;
import com.eduessence.cursos.model.dto.response.CursoCheckinResponse;
import com.eduessence.cursos.model.dto.response.SesionCheckinResponse;
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
import com.eduessence.cursos.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final SesionVirtualRepository sesionRepo;
    private final CursoRepository cursoRepo;
    private final MatriculaRepository matriculaRepo;
    private final AsistenciaPresencialRepository asistenciaRepo;
    private final AuthenticateServiceClient authClient;

    @Override
    @Transactional(readOnly = true)
    public List<SesionCheckinResponse> sesionesDeDia(LocalDate fecha, Long soloParaUsuarioId) {
        LocalDate f = fecha == null ? LocalDate.now() : fecha;
        LocalDateTime desde = f.atStartOfDay();
        LocalDateTime hasta = f.atTime(23, 59, 59);

        List<SesionVirtual> sesiones = sesionRepo
                .findAllByTipoAndFechaInicioBetweenOrderByFechaInicioAsc(
                        TipoSesion.PRESENCIAL, desde, hasta);

        // Modo asistente: filtrar a las sesiones cuyos cursos tienen matrícula
        // PRESENCIAL activa del user + resolver su propio estado por sesión.
        java.util.Map<Long, Matricula> matriculaPorCurso = java.util.Map.of();
        if (soloParaUsuarioId != null) {
            matriculaPorCurso = matriculaRepo
                    .findAllByUsuarioId(soloParaUsuarioId).stream()
                    .filter(this::tienePresencialActivo)
                    .collect(java.util.stream.Collectors.toMap(
                            m -> m.getCurso().getId(), m -> m, (a, b) -> a));
            java.util.Set<Long> cursosDelUser = matriculaPorCurso.keySet();
            sesiones = sesiones.stream()
                    .filter(s -> cursosDelUser.contains(s.getCursoId()))
                    .toList();
        }

        final java.util.Map<Long, Matricula> matriculasFinal = matriculaPorCurso;
        return sesiones.stream().map((SesionVirtual s) -> {
            SesionCheckinResponse resp = toSesionResponse(s);
            if (soloParaUsuarioId != null) {
                Matricula mia = matriculasFinal.get(s.getCursoId());
                if (mia != null) {
                    AsistenciaPresencial ya = asistenciaRepo
                            .findByMatriculaIdAndSesionVirtualId(mia.getId(), s.getId())
                            .orElse(null);
                    resp.setMiCheckin(ya != null);
                    resp.setMiFechaCheckin(ya == null ? null : ya.getFechaCheckin());
                } else {
                    resp.setMiCheckin(false);
                }
            }
            return resp;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AsistenteCheckinResponse miAsistenciaEnSesion(Long sesionId, Long usuarioId) {
        SesionVirtual sesion = sesionRepo.findById(sesionId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Sesión no existe"));

        Matricula matricula = matriculaRepo.findAllByUsuarioId(usuarioId).stream()
                .filter(m -> sesion.getCursoId().equals(m.getCurso().getId()))
                .filter(this::tienePresencialActivo)
                .findFirst().orElse(null);
        if (matricula == null) return null;

        AsistenciaPresencial ya = asistenciaRepo
                .findByMatriculaIdAndSesionVirtualId(matricula.getId(), sesionId)
                .orElse(null);
        Map<String, Object> u = lookupUsuarios(java.util.List.of(usuarioId))
                .getOrDefault(usuarioId, Map.of());
        String nombres = strOr(u, "nombres", "");
        String apellidos = strOr(u, "apellidos", "");
        String nombre = (nombres + " " + apellidos).trim();

        return AsistenteCheckinResponse.builder()
                .matriculaId(matricula.getId())
                .usuarioId(usuarioId)
                .nombreCompleto(nombre.isEmpty() ? "Asistente #" + usuarioId : nombre)
                .email(strOr(u, "email", ""))
                .telefono(strOr(u, "telefono", ""))
                .documento(strOr(u, "documento", ""))
                .escarapelaToken(matricula.getEscarapelaToken())
                .checkinHecho(ya != null)
                .fechaCheckin(ya == null ? null : ya.getFechaCheckin())
                .build();
    }

    private SesionCheckinResponse toSesionResponse(SesionVirtual s) {
        Curso curso = cursoRepo.findById(s.getCursoId())
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO,
                        "Curso de la sesión no existe: " + s.getCursoId()));
        String sede = curso.getModalidades().stream()
                .filter(cm -> Boolean.TRUE.equals(cm.getActivo())
                        && cm.getTipo() == ModalidadTipo.PRESENCIAL)
                .map(CursoModalidad::getSede)
                .filter(x -> x != null && !x.isBlank())
                .findFirst().orElse(s.getUbicacionTexto());

        long totalMatriculados = matriculaRepo
                .findAllByCurso_Id(curso.getId()).stream()
                .filter(this::tienePresencialActivo)
                .count();
        long totalCheckin = asistenciaRepo.countBySesionVirtualId(s.getId());

        return SesionCheckinResponse.builder()
                .sesionId(s.getId())
                .cursoId(curso.getId())
                .cursoNombre(curso.getNombre())
                .titulo(s.getTitulo())
                .fechaInicio(s.getFechaInicio())
                .duracionMinutos(s.getDuracionMinutos())
                .sede(sede)
                .cancelada(Boolean.TRUE.equals(s.getCancelada()))
                .qrToken(s.getQrToken())
                .totalMatriculados(totalMatriculados)
                .totalCheckin(totalCheckin)
                .build();
    }

    private boolean tienePresencialActivo(Matricula m) {
        Set<ModalidadTipo> mods = m.getModalidades();
        if (mods == null || !mods.contains(ModalidadTipo.PRESENCIAL)) return false;
        EstadoMatricula e = m.getEstado();
        return e == EstadoMatricula.ACTIVA
                || e == EstadoMatricula.APROBADA
                || e == EstadoMatricula.FINALIZADA;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenteCheckinResponse> asistentesDeSesion(Long sesionId) {
        SesionVirtual sesion = sesionRepo.findById(sesionId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Sesión no existe"));

        List<Matricula> matriculados = matriculaRepo
                .findAllByCurso_Id(sesion.getCursoId()).stream()
                .filter(this::tienePresencialActivo)
                .toList();

        // Estado check-in
        Map<Long, AsistenciaPresencial> yaHizo = new HashMap<>();
        for (AsistenciaPresencial a : asistenciaRepo.findAllBySesionVirtualId(sesionId)) {
            yaHizo.put(a.getMatriculaId(), a);
        }

        // Batch lookup de usuarios
        List<Long> userIds = matriculados.stream().map(Matricula::getUsuarioId).toList();
        Map<Long, Map<String, Object>> usuariosPorId = lookupUsuarios(userIds);

        return matriculados.stream().map(m -> {
            AsistenciaPresencial ya = yaHizo.get(m.getId());
            Map<String, Object> u = usuariosPorId.get(m.getUsuarioId());
            String nombres   = strOr(u, "nombres", "");
            String apellidos = strOr(u, "apellidos", "");
            String nombre = (nombres + " " + apellidos).trim();

            return AsistenteCheckinResponse.builder()
                    .matriculaId(m.getId())
                    .usuarioId(m.getUsuarioId())
                    .nombreCompleto(nombre.isEmpty() ? "Asistente #" + m.getUsuarioId() : nombre)
                    .email(strOr(u, "email", ""))
                    .telefono(strOr(u, "telefono", ""))
                    .documento(strOr(u, "documento", ""))
                    .escarapelaToken(m.getEscarapelaToken())
                    .checkinHecho(ya != null)
                    .fechaCheckin(ya == null ? null : ya.getFechaCheckin())
                    .build();
        })
        .sorted((a, b) -> a.getNombreCompleto().compareToIgnoreCase(b.getNombreCompleto()))
        .toList();
    }

    @Override
    @Transactional
    public CheckinScanResponse scan(CheckinScanRequest req, Long operadorUsuarioId) {
        // 1) Resolver sesión (primero, para poder buscar matrícula del usuario JWT en ese curso)
        SesionVirtual sesion;
        if (req.getSesionId() != null) {
            sesion = sesionRepo.findById(req.getSesionId()).orElse(null);
        } else if (req.getQrToken() != null && !req.getQrToken().isBlank()) {
            sesion = sesionRepo.findByQrToken(req.getQrToken()).orElse(null);
        } else {
            return CheckinScanResponse.builder()
                    .estado("DATOS_INVALIDOS")
                    .mensaje("Faltan datos: enviá sesionId o qrToken")
                    .build();
        }
        if (sesion == null) {
            return CheckinScanResponse.builder()
                    .estado("SESION_NO_ENCONTRADA")
                    .mensaje("La sesión no existe")
                    .build();
        }

        // 2) Resolver matrícula
        Matricula matricula = null;
        if (req.getEscarapelaToken() != null && !req.getEscarapelaToken().isBlank()) {
            matricula = matriculaRepo.findByEscarapelaToken(req.getEscarapelaToken())
                    .orElse(null);
        } else if (req.getMatriculaId() != null) {
            matricula = matriculaRepo.findById(req.getMatriculaId()).orElse(null);
        } else if (operadorUsuarioId != null) {
            // Self-scan: el asistente logueado escaneó el QR de la sesión.
            // Buscamos su matrícula en el curso de esa sesión.
            matricula = matriculaRepo
                    .findAllByUsuarioId(operadorUsuarioId).stream()
                    .filter(m -> m.getCurso() != null
                            && sesion.getCursoId().equals(m.getCurso().getId()))
                    .findFirst().orElse(null);
            if (matricula == null) {
                return CheckinScanResponse.builder()
                        .estado("MATRICULA_NO_PERTENECE")
                        .mensaje("No estás matriculado en el curso de esta sesión")
                        .build();
            }
            // Self-scan → el operador es el propio asistente → no lo guardamos
            // como operador (opcional; lo dejamos null para distinguir del scan-admin).
            operadorUsuarioId = null;
        } else {
            return CheckinScanResponse.builder()
                    .estado("DATOS_INVALIDOS")
                    .mensaje("Faltan datos: enviá escarapelaToken, matriculaId o autenticate")
                    .build();
        }
        if (matricula == null) {
            return CheckinScanResponse.builder()
                    .estado("MATRICULA_NO_ENCONTRADA")
                    .mensaje("No encontramos la matrícula del asistente")
                    .build();
        }
        if (Boolean.TRUE.equals(sesion.getCancelada())) {
            return CheckinScanResponse.builder()
                    .estado("SESION_CANCELADA")
                    .mensaje("La sesión está cancelada")
                    .build();
        }

        // 3) Validar que la matrícula sea del curso de la sesión
        if (!sesion.getCursoId().equals(matricula.getCurso().getId())) {
            return CheckinScanResponse.builder()
                    .estado("MATRICULA_NO_PERTENECE")
                    .mensaje("El asistente no está matriculado en este curso")
                    .build();
        }
        if (!tienePresencialActivo(matricula)) {
            return CheckinScanResponse.builder()
                    .estado("SIN_ACCESO_PRESENCIAL")
                    .mensaje("La matrícula no tiene acceso PRESENCIAL activo")
                    .build();
        }

        // 4) Idempotencia — si ya está registrado, devolver YA_REGISTRADO
        AsistenciaPresencial existente = asistenciaRepo
                .findByMatriculaIdAndSesionVirtualId(matricula.getId(), sesion.getId())
                .orElse(null);
        Map<String, Object> u = lookupUsuarios(List.of(matricula.getUsuarioId()))
                .getOrDefault(matricula.getUsuarioId(), Map.of());
        String nombre = (strOr(u, "nombres", "") + " " + strOr(u, "apellidos", "")).trim();

        if (existente != null) {
            return CheckinScanResponse.builder()
                    .estado("YA_REGISTRADO")
                    .mensaje("Ya había hecho check-in a las "
                            + existente.getFechaCheckin())
                    .matriculaId(matricula.getId())
                    .sesionId(sesion.getId())
                    .nombreCompleto(nombre)
                    .email(strOr(u, "email", ""))
                    .fechaCheckin(existente.getFechaCheckin())
                    .totalCheckinSesion(asistenciaRepo.countBySesionVirtualId(sesion.getId()))
                    .build();
        }

        AsistenciaPresencial nueva = asistenciaRepo.save(AsistenciaPresencial.builder()
                .matriculaId(matricula.getId())
                .sesionVirtualId(sesion.getId())
                .fechaCheckin(LocalDateTime.now())
                .operadorUsuarioId(operadorUsuarioId)
                .build());
        log.info("Check-in registrado: matricula={} sesion={} operador={}",
                matricula.getId(), sesion.getId(), operadorUsuarioId);

        return CheckinScanResponse.builder()
                .estado("OK")
                .mensaje("Check-in registrado")
                .matriculaId(matricula.getId())
                .sesionId(sesion.getId())
                .nombreCompleto(nombre)
                .email(strOr(u, "email", ""))
                .fechaCheckin(nueva.getFechaCheckin())
                .totalCheckinSesion(asistenciaRepo.countBySesionVirtualId(sesion.getId()))
                .build();
    }

    /* ─────────── helpers ─────────── */

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
                        if (idObj instanceof Number n) {
                            out.put(n.longValue(), m);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Lookup batch usuarios falló: {}", ex.getMessage());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoCheckinResponse> misCursosPresenciales(Long soloParaUsuarioId) {
        // Determinar el set de curso_ids: los del user (asistente) o todos los
        // que tengan al menos una sesión PRESENCIAL agendada (operador).
        java.util.Set<Long> cursoIds;
        java.util.Map<Long, Matricula> matriculaPorCurso = java.util.Map.of();
        if (soloParaUsuarioId != null) {
            matriculaPorCurso = matriculaRepo
                    .findAllByUsuarioId(soloParaUsuarioId).stream()
                    .filter(this::tienePresencialActivo)
                    .collect(java.util.stream.Collectors.toMap(
                            m -> m.getCurso().getId(), m -> m, (a, b) -> a));
            cursoIds = matriculaPorCurso.keySet();
        } else {
            // Operador: cursos que tienen al menos una sesión PRESENCIAL
            cursoIds = sesionRepo.findAll().stream()
                    .filter(s -> s.getTipo() == TipoSesion.PRESENCIAL)
                    .map(SesionVirtual::getCursoId)
                    .collect(java.util.stream.Collectors.toSet());
        }

        if (cursoIds.isEmpty()) return List.of();

        List<CursoCheckinResponse> out = new java.util.ArrayList<>();
        for (Long cursoId : cursoIds) {
            Curso c = cursoRepo.findById(cursoId).orElse(null);
            if (c == null) continue;

            List<SesionVirtual> sesionesPres = sesionRepo
                    .findAllByCursoIdOrderByFechaInicioAsc(cursoId).stream()
                    .filter(s -> s.getTipo() == TipoSesion.PRESENCIAL)
                    .toList();
            if (sesionesPres.isEmpty()) continue;

            String sede = c.getModalidades().stream()
                    .filter(cm -> Boolean.TRUE.equals(cm.getActivo())
                            && cm.getTipo() == ModalidadTipo.PRESENCIAL)
                    .map(CursoModalidad::getSede)
                    .filter(x -> x != null && !x.isBlank())
                    .findFirst().orElse(null);

            long totalMatriculados = matriculaRepo
                    .findAllByCurso_Id(cursoId).stream()
                    .filter(this::tienePresencialActivo)
                    .count();

            long totalCheckins = sesionesPres.stream()
                    .mapToLong(s -> asistenciaRepo.countBySesionVirtualId(s.getId()))
                    .sum();

            Integer misCheckins = null;
            if (soloParaUsuarioId != null) {
                Matricula mia = matriculaPorCurso.get(cursoId);
                if (mia != null) {
                    int count = 0;
                    for (SesionVirtual s : sesionesPres) {
                        if (asistenciaRepo.findByMatriculaIdAndSesionVirtualId(
                                mia.getId(), s.getId()).isPresent()) count++;
                    }
                    misCheckins = count;
                } else {
                    misCheckins = 0;
                }
            }

            out.add(CursoCheckinResponse.builder()
                    .cursoId(c.getId())
                    .nombre(c.getNombre())
                    .slug(c.getSlug())
                    .sede(sede)
                    .fechaInicio(c.getFechaInicio())
                    .fechaFin(c.getFechaFin())
                    .totalSesiones(sesionesPres.size())
                    .totalCheckins(totalCheckins)
                    .totalMatriculados(totalMatriculados)
                    .misCheckins(misCheckins)
                    .build());
        }

        out.sort((a, b) -> {
            if (a.getFechaInicio() == null && b.getFechaInicio() == null) return 0;
            if (a.getFechaInicio() == null) return 1;
            if (b.getFechaInicio() == null) return -1;
            return a.getFechaInicio().compareTo(b.getFechaInicio());
        });
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCheckinResponse> sesionesDelCurso(Long cursoId, Long soloParaUsuarioId) {
        List<SesionVirtual> sesiones = sesionRepo
                .findAllByCursoIdOrderByFechaInicioAsc(cursoId).stream()
                .filter(s -> s.getTipo() == TipoSesion.PRESENCIAL)
                .toList();
        if (sesiones.isEmpty()) return List.of();

        Matricula mia = null;
        if (soloParaUsuarioId != null) {
            mia = matriculaRepo.findAllByUsuarioId(soloParaUsuarioId).stream()
                    .filter(m -> cursoId.equals(m.getCurso().getId()))
                    .filter(this::tienePresencialActivo)
                    .findFirst().orElse(null);
        }
        final Matricula miaFinal = mia;

        return sesiones.stream().map(s -> {
            SesionCheckinResponse r = toSesionResponse(s);
            if (soloParaUsuarioId != null) {
                if (miaFinal != null) {
                    AsistenciaPresencial ya = asistenciaRepo
                            .findByMatriculaIdAndSesionVirtualId(miaFinal.getId(), s.getId())
                            .orElse(null);
                    r.setMiCheckin(ya != null);
                    r.setMiFechaCheckin(ya == null ? null : ya.getFechaCheckin());
                } else {
                    r.setMiCheckin(false);
                }
            }
            return r;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinItemResponse> checkinsDelCurso(Long cursoId) {
        List<SesionVirtual> sesiones = sesionRepo
                .findAllByCursoIdOrderByFechaInicioAsc(cursoId).stream()
                .filter(s -> s.getTipo() == TipoSesion.PRESENCIAL)
                .toList();
        if (sesiones.isEmpty()) return List.of();

        // Todas las asistencias de las sesiones del curso
        List<AsistenciaPresencial> asistencias = new java.util.ArrayList<>();
        java.util.Map<Long, SesionVirtual> sesionPorId = new java.util.HashMap<>();
        for (SesionVirtual s : sesiones) {
            sesionPorId.put(s.getId(), s);
            asistencias.addAll(asistenciaRepo.findAllBySesionVirtualId(s.getId()));
        }
        if (asistencias.isEmpty()) return List.of();

        // Resolver matrículas + usuarios en batch
        java.util.Set<Long> matriculaIds = asistencias.stream()
                .map(AsistenciaPresencial::getMatriculaId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, Matricula> matriculaPorId = new java.util.HashMap<>();
        for (Matricula m : matriculaRepo.findAllById(matriculaIds)) {
            matriculaPorId.put(m.getId(), m);
        }
        java.util.List<Long> usuarioIds = matriculaPorId.values().stream()
                .map(Matricula::getUsuarioId).distinct().toList();
        java.util.Map<Long, Map<String, Object>> usuarios = lookupUsuarios(usuarioIds);

        return asistencias.stream().map(a -> {
            Matricula m = matriculaPorId.get(a.getMatriculaId());
            SesionVirtual s = sesionPorId.get(a.getSesionVirtualId());
            Map<String, Object> u = m == null ? Map.of()
                    : usuarios.getOrDefault(m.getUsuarioId(), Map.of());
            String nombre = (strOr(u, "nombres", "") + " "
                    + strOr(u, "apellidos", "")).trim();
            return CheckinItemResponse.builder()
                    .asistenciaId(a.getId())
                    .matriculaId(a.getMatriculaId())
                    .usuarioId(m == null ? null : m.getUsuarioId())
                    .nombreCompleto(nombre.isEmpty() ? "Asistente" : nombre)
                    .email(strOr(u, "email", ""))
                    .sesionId(a.getSesionVirtualId())
                    .sesionTitulo(s == null ? "" : s.getTitulo())
                    .sesionFechaInicio(s == null ? null : s.getFechaInicio())
                    .fechaCheckin(a.getFechaCheckin())
                    .build();
        })
        .sorted((x, y) -> {
            if (x.getFechaCheckin() == null && y.getFechaCheckin() == null) return 0;
            if (x.getFechaCheckin() == null) return 1;
            if (y.getFechaCheckin() == null) return -1;
            return y.getFechaCheckin().compareTo(x.getFechaCheckin()); // DESC
        })
        .toList();
    }

    private static String strOr(Map<String, Object> m, String k, String def) {
        if (m == null || m.get(k) == null) return def;
        return m.get(k).toString();
    }
}
