package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.CrearRecursoRequest;
import com.eduessence.cursos.model.dto.response.RecursoResponse;
import com.eduessence.cursos.model.entity.CursoRecurso;
import com.eduessence.cursos.model.enums.TipoRecurso;
import com.eduessence.cursos.repository.CursoRecursoRepository;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.service.CursoRecursoService;
import com.eduessence.cursos.service.S3PresignedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CursoRecursoServiceImpl implements CursoRecursoService {

    private final CursoRecursoRepository repo;
    private final CursoRepository cursoRepo;
    private final S3PresignedService s3;

    @Override
    @Transactional(readOnly = true)
    public List<RecursoResponse> listar(Long cursoId) {
        validarCurso(cursoId);
        return repo.findByCursoIdOrderByOrdenAsc(cursoId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoResponse> listarPorTipo(Long cursoId, TipoRecurso tipo) {
        validarCurso(cursoId);
        return repo.findByCursoIdAndTipoOrderByOrdenAsc(cursoId, tipo)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public RecursoResponse crear(Long cursoId, CrearRecursoRequest req) {
        validarCurso(cursoId);

        int orden = req.getOrden() != null
                ? req.getOrden()
                : (int) repo.countByCursoIdAndTipo(cursoId, req.getTipo());

        CursoRecurso r = CursoRecurso.builder()
                .cursoId(cursoId)
                .tipo(req.getTipo())
                .titulo(req.getTitulo())
                .descripcion(req.getDescripcion())
                .s3Key(req.getS3Key())
                .urlPublica(req.getUrlPublica())
                .mime(req.getMime())
                .tamanoBytes(req.getTamanoBytes())
                .nombreOriginal(req.getNombreOriginal())
                .orden(orden)
                .build();
        return toDto(repo.save(r));
    }

    @Override
    @Transactional
    public RecursoResponse actualizar(Long cursoId, Long recursoId, CrearRecursoRequest req) {
        CursoRecurso r = findOrThrow(cursoId, recursoId);
        r.setTipo(req.getTipo());
        r.setTitulo(req.getTitulo());
        r.setDescripcion(req.getDescripcion());
        if (req.getOrden() != null) r.setOrden(req.getOrden());

        // Si el s3Key cambió, borramos el viejo objeto y guardamos el nuevo
        if (req.getS3Key() != null && !req.getS3Key().equals(r.getS3Key())) {
            String anterior = r.getS3Key();
            r.setS3Key(req.getS3Key());
            r.setUrlPublica(req.getUrlPublica());
            r.setMime(req.getMime());
            r.setTamanoBytes(req.getTamanoBytes());
            r.setNombreOriginal(req.getNombreOriginal());
            s3.borrar(anterior);
        }
        return toDto(repo.save(r));
    }

    @Override
    @Transactional
    public void borrar(Long cursoId, Long recursoId) {
        CursoRecurso r = findOrThrow(cursoId, recursoId);
        s3.borrar(r.getS3Key());
        repo.delete(r);
    }

    @Override
    @Transactional
    public void reordenar(Long cursoId, List<Long> recursoIdsEnOrden) {
        validarCurso(cursoId);
        if (recursoIdsEnOrden == null) return;
        List<CursoRecurso> recursos = repo.findAllById(recursoIdsEnOrden);
        for (CursoRecurso r : recursos) {
            if (!cursoId.equals(r.getCursoId())) {
                throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Recurso " + r.getId() + " no pertenece al curso " + cursoId);
            }
            int idx = recursoIdsEnOrden.indexOf(r.getId());
            r.setOrden(idx);
        }
        repo.saveAll(recursos);
    }

    /* helpers */

    private void validarCurso(Long cursoId) {
        if (!cursoRepo.existsById(cursoId)) {
            throw new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO);
        }
    }

    private CursoRecurso findOrThrow(Long cursoId, Long recursoId) {
        CursoRecurso r = repo.findById(recursoId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Recurso no encontrado"));
        if (!cursoId.equals(r.getCursoId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El recurso no pertenece al curso indicado");
        }
        return r;
    }

    private RecursoResponse toDto(CursoRecurso r) {
        return RecursoResponse.builder()
                .id(r.getId())
                .cursoId(r.getCursoId())
                .tipo(r.getTipo())
                .titulo(r.getTitulo())
                .descripcion(r.getDescripcion())
                .s3Key(r.getS3Key())
                .urlPublica(r.getUrlPublica())
                .mime(r.getMime())
                .tamanoBytes(r.getTamanoBytes())
                .nombreOriginal(r.getNombreOriginal())
                .orden(r.getOrden())
                .fechaCreacion(r.getFechaCreacion())
                .build();
    }
}
