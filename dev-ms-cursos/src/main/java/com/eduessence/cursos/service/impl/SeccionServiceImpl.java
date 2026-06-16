package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.CrearSeccionRequest;
import com.eduessence.cursos.model.dto.response.LeccionResponse;
import com.eduessence.cursos.model.dto.response.SeccionResponse;
import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.entity.Leccion;
import com.eduessence.cursos.model.entity.Seccion;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.SeccionRepository;
import com.eduessence.cursos.service.SeccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeccionServiceImpl implements SeccionService {

    private final SeccionRepository repo;
    private final CursoRepository cursoRepo;

    @Override
    @Transactional(readOnly = true)
    public List<SeccionResponse> listar(Long cursoId) {
        Curso curso = findCurso(cursoId);
        return curso.getSecciones().stream()
                .sorted(Comparator.comparingInt(Seccion::getOrden))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SeccionResponse crear(Long cursoId, CrearSeccionRequest req) {
        Curso curso = findCurso(cursoId);
        int orden = req.getOrden() != null ? req.getOrden() : curso.getSecciones().size();
        Seccion s = Seccion.builder()
                .curso(curso)
                .titulo(req.getTitulo())
                .descripcion(req.getDescripcion())
                .orden(orden)
                .bloqueante(Boolean.TRUE.equals(req.getBloqueante()))
                .build();
        return toDto(repo.save(s));
    }

    @Override
    @Transactional
    public SeccionResponse actualizar(Long cursoId, Long seccionId, CrearSeccionRequest req) {
        Seccion s = findOrThrow(cursoId, seccionId);
        if (req.getTitulo() != null) s.setTitulo(req.getTitulo());
        if (req.getDescripcion() != null) s.setDescripcion(req.getDescripcion());
        if (req.getOrden() != null) s.setOrden(req.getOrden());
        if (req.getBloqueante() != null) s.setBloqueante(req.getBloqueante());
        return toDto(repo.save(s));
    }

    @Override
    @Transactional
    public void borrar(Long cursoId, Long seccionId) {
        repo.delete(findOrThrow(cursoId, seccionId));
    }

    @Override
    @Transactional
    public void reordenar(Long cursoId, List<Long> ids) {
        if (ids == null) return;
        List<Seccion> secciones = repo.findAllById(ids);
        for (Seccion s : secciones) {
            if (s.getCurso() == null || !cursoId.equals(s.getCurso().getId())) {
                throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Sección " + s.getId() + " no pertenece al curso");
            }
            s.setOrden(ids.indexOf(s.getId()));
        }
        repo.saveAll(secciones);
    }

    /* helpers */

    private Curso findCurso(Long cursoId) {
        return cursoRepo.findById(cursoId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO));
    }

    private Seccion findOrThrow(Long cursoId, Long seccionId) {
        Seccion s = repo.findById(seccionId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Sección no encontrada"));
        if (s.getCurso() == null || !cursoId.equals(s.getCurso().getId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "La sección no pertenece al curso");
        }
        return s;
    }

    private SeccionResponse toDto(Seccion s) {
        List<LeccionResponse> lecs = s.getLecciones() == null ? List.of()
                : s.getLecciones().stream()
                .sorted(Comparator.comparingInt(Leccion::getOrden))
                .map(this::leccionToDto)
                .toList();
        return SeccionResponse.builder()
                .id(s.getId())
                .cursoId(s.getCurso() != null ? s.getCurso().getId() : null)
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .orden(s.getOrden())
                .bloqueante(s.getBloqueante())
                .lecciones(lecs)
                .build();
    }

    private LeccionResponse leccionToDto(Leccion l) {
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
