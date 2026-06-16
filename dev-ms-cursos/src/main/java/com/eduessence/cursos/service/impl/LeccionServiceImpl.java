package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.CrearLeccionRequest;
import com.eduessence.cursos.model.dto.response.LeccionResponse;
import com.eduessence.cursos.model.entity.Leccion;
import com.eduessence.cursos.model.entity.Seccion;
import com.eduessence.cursos.repository.LeccionRepository;
import com.eduessence.cursos.repository.SeccionRepository;
import com.eduessence.cursos.service.LeccionService;
import com.eduessence.cursos.service.ProgresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeccionServiceImpl implements LeccionService {

    private final LeccionRepository repo;
    private final SeccionRepository seccionRepo;
    private final ProgresoService progresoService;

    @Override
    @Transactional(readOnly = true)
    public List<LeccionResponse> listar(Long seccionId) {
        Seccion s = findSeccion(seccionId);
        return s.getLecciones().stream()
                .sorted(Comparator.comparingInt(Leccion::getOrden))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public LeccionResponse crear(Long seccionId, CrearLeccionRequest req) {
        Seccion s = findSeccion(seccionId);
        int orden = req.getOrden() != null ? req.getOrden() : s.getLecciones().size();
        Leccion l = Leccion.builder()
                .seccion(s)
                .titulo(req.getTitulo())
                .descripcion(req.getDescripcion())
                .tipo(req.getTipo())
                .orden(orden)
                .bloqueante(Boolean.TRUE.equals(req.getBloqueante()))
                .duracionSegundos(req.getDuracionSegundos())
                .recursoUrl(req.getRecursoUrl())
                .contenidoTexto(req.getContenidoTexto())
                .examenId(req.getExamenId())
                .actividadId(req.getActividadId())
                .sesionVirtualId(req.getSesionVirtualId())
                .build();
        LeccionResponse dto = toDto(repo.save(l));
        // Sube el total de lecciones del curso → recalcular % de matrículas existentes
        progresoService.recalcularProgresoCurso(s.getCurso().getId());
        return dto;
    }

    @Override
    @Transactional
    public LeccionResponse actualizar(Long seccionId, Long leccionId, CrearLeccionRequest req) {
        Leccion l = findOrThrow(seccionId, leccionId);
        if (req.getTitulo() != null) l.setTitulo(req.getTitulo());
        if (req.getDescripcion() != null) l.setDescripcion(req.getDescripcion());
        if (req.getTipo() != null) l.setTipo(req.getTipo());
        if (req.getOrden() != null) l.setOrden(req.getOrden());
        if (req.getBloqueante() != null) l.setBloqueante(req.getBloqueante());
        if (req.getDuracionSegundos() != null) l.setDuracionSegundos(req.getDuracionSegundos());
        if (req.getRecursoUrl() != null) l.setRecursoUrl(req.getRecursoUrl());
        if (req.getContenidoTexto() != null) l.setContenidoTexto(req.getContenidoTexto());
        if (req.getExamenId() != null) l.setExamenId(req.getExamenId());
        if (req.getActividadId() != null) l.setActividadId(req.getActividadId());
        if (req.getSesionVirtualId() != null) l.setSesionVirtualId(req.getSesionVirtualId());
        return toDto(repo.save(l));
    }

    @Override
    @Transactional
    public void borrar(Long seccionId, Long leccionId) {
        Leccion l = findOrThrow(seccionId, leccionId);
        Long cursoId = l.getSeccion().getCurso().getId();
        repo.delete(l);
        // Forzamos el DELETE para que MySQL aplique el ON DELETE CASCADE sobre
        // progreso_leccion ANTES de recontar. Si no, el countByMatriculaIdAndEstado
        // todavía vería las filas y el % saldría con el total viejo.
        repo.flush();
        progresoService.recalcularProgresoCurso(cursoId);
    }

    @Override
    @Transactional
    public void reordenar(Long seccionId, List<Long> ids) {
        if (ids == null) return;
        List<Leccion> lecs = repo.findAllById(ids);
        for (Leccion l : lecs) {
            if (l.getSeccion() == null || !seccionId.equals(l.getSeccion().getId())) {
                throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Lección " + l.getId() + " no pertenece a la sección");
            }
            l.setOrden(ids.indexOf(l.getId()));
        }
        repo.saveAll(lecs);
    }

    /* helpers */

    private Seccion findSeccion(Long id) {
        return seccionRepo.findById(id).orElseThrow(() ->
                new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS, "Sección no encontrada"));
    }

    private Leccion findOrThrow(Long seccionId, Long leccionId) {
        Leccion l = repo.findById(leccionId).orElseThrow(() ->
                new CursosApiException(ServerApiStatusCode.LECCION_NO_ENCONTRADA));
        if (l.getSeccion() == null || !seccionId.equals(l.getSeccion().getId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "La lección no pertenece a esa sección");
        }
        return l;
    }

    private LeccionResponse toDto(Leccion l) {
        return LeccionResponse.builder()
                .id(l.getId())
                .seccionId(l.getSeccion() != null ? l.getSeccion().getId() : null)
                .titulo(l.getTitulo())
                .descripcion(l.getDescripcion())
                .tipo(l.getTipo())
                .orden(l.getOrden())
                .bloqueante(l.getBloqueante())
                .duracionSegundos(l.getDuracionSegundos())
                .recursoUrl(l.getRecursoUrl())
                .contenidoTexto(l.getContenidoTexto())
                .examenId(l.getExamenId())
                .actividadId(l.getActividadId())
                .sesionVirtualId(l.getSesionVirtualId())
                .build();
    }
}
