package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.model.dto.response.EventoResponse;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.SesionVirtual;
import com.eduessence.cursos.model.enums.EstadoCurso;
import com.eduessence.cursos.model.enums.TipoSesion;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.SesionVirtualRepository;
import com.eduessence.cursos.service.EventosService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventosServiceImpl implements EventosService {

    private final SesionVirtualRepository sesionRepo;
    private final CursoRepository cursoRepo;

    @Override
    @Transactional(readOnly = true)
    public List<EventoResponse> listarPublicosProximos(int horizonteDias, int limite) {
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusDays(Math.max(1, horizonteDias));

        // Solo cursos ACTIVOS son visibles públicamente
        Set<Long> cursoIdsActivos = cursoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCurso.ACTIVO)
                .map(Curso::getId)
                .collect(Collectors.toSet());
        if (cursoIdsActivos.isEmpty()) return List.of();

        Specification<SesionVirtual> spec = (root, query, cb) -> cb.and(
                cb.between(root.get("fechaInicio"), desde, hasta),
                cb.equal(cb.coalesce(root.get("cancelada"), false), false),
                root.get("cursoId").in(cursoIdsActivos)
        );
        List<SesionVirtual> sesiones = sesionRepo.findAll(
                        spec,
                        PageRequest.of(0, Math.max(1, limite), Sort.by("fechaInicio").ascending()))
                .getContent();
        return mapToEventos(sesiones);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoResponse> listarAdmin(LocalDateTime desde, LocalDateTime hasta,
                                            TipoSesion tipo, Long cursoId,
                                            String search, Boolean incluirCanceladas) {
        Specification<SesionVirtual> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (desde != null) preds.add(cb.greaterThanOrEqualTo(root.get("fechaInicio"), desde));
            if (hasta != null) preds.add(cb.lessThanOrEqualTo(root.get("fechaInicio"), hasta));
            if (tipo != null) preds.add(cb.equal(root.get("tipo"), tipo));
            if (cursoId != null) preds.add(cb.equal(root.get("cursoId"), cursoId));
            if (Boolean.FALSE.equals(incluirCanceladas) || incluirCanceladas == null) {
                preds.add(cb.equal(cb.coalesce(root.get("cancelada"), false), false));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("descripcion"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("ubicacionTexto"), "")), like)
                ));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
        };
        return mapToEventos(sesionRepo.findAll(spec, Sort.by("fechaInicio").ascending()));
    }

    /* ────────── helpers ────────── */

    /**
     * Carga los cursos relacionados en un solo viaje a BD y arma los DTOs.
     * Sin estado compartido entre peticiones.
     */
    private List<EventoResponse> mapToEventos(List<SesionVirtual> sesiones) {
        if (sesiones.isEmpty()) return List.of();
        Set<Long> ids = sesiones.stream().map(SesionVirtual::getCursoId).collect(Collectors.toSet());
        Map<Long, Curso> cursos = cursoRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Curso::getId, c -> c));
        return sesiones.stream()
                .map(s -> toEvento(s, cursos.get(s.getCursoId())))
                .toList();
    }

    private EventoResponse toEvento(SesionVirtual s, Curso c) {
        return EventoResponse.builder()
                .id(s.getId())
                .tipo(s.getTipo())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .fechaInicio(s.getFechaInicio())
                .duracionMinutos(s.getDuracionMinutos())
                .ubicacionTexto(s.getUbicacionTexto())
                .playbackUrl(s.getPlaybackUrl())
                .recordingUrl(s.getRecordingUrl())
                .cursoId(s.getCursoId())
                .cursoNombre(c == null ? null : c.getNombre())
                .cursoSlug(c == null ? null : c.getSlug())
                .cursoLogoUrl(c == null ? null : c.getLogoUrl())
                .cancelada(Boolean.TRUE.equals(s.getCancelada()))
                .razonCancelacion(s.getRazonCancelacion())
                .build();
    }
}
