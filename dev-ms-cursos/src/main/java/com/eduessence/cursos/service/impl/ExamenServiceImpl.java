package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.CrearExamenRequest;
import com.eduessence.cursos.model.dto.request.OpcionRequest;
import com.eduessence.cursos.model.dto.request.PreguntaRequest;
import com.eduessence.cursos.model.dto.response.ExamenResponse;
import com.eduessence.cursos.model.dto.response.OpcionResponse;
import com.eduessence.cursos.model.dto.response.PreguntaResponse;
import com.eduessence.cursos.model.entity.Examen;
import com.eduessence.cursos.model.entity.OpcionPregunta;
import com.eduessence.cursos.model.entity.PreguntaExamen;
import com.eduessence.cursos.repository.CursoRepository;
import com.eduessence.cursos.repository.ExamenRepository;
import com.eduessence.cursos.service.ExamenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamenServiceImpl implements ExamenService {

    private final ExamenRepository examenRepo;
    private final CursoRepository cursoRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ExamenResponse> listarPorCurso(Long cursoId) {
        validarCurso(cursoId);
        return examenRepo.findAll().stream()
                .filter(e -> cursoId.equals(e.getCursoId()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamenResponse obtener(Long cursoId, Long examenId) {
        return toDto(findOrThrow(cursoId, examenId));
    }

    @Override
    @Transactional
    public ExamenResponse crear(Long cursoId, CrearExamenRequest req) {
        validarCurso(cursoId);
        Examen e = Examen.builder()
                .cursoId(cursoId)
                .titulo(req.getTitulo())
                .descripcion(req.getDescripcion())
                .duracionMinutos(req.getDuracionMinutos())
                .intentosMaximos(req.getIntentosMaximos() == null ? 1 : req.getIntentosMaximos())
                .notaMinimaPropia(req.getNotaMinimaPropia())
                .shufflePreguntas(Boolean.TRUE.equals(req.getShufflePreguntas()))
                .shuffleOpciones(Boolean.TRUE.equals(req.getShuffleOpciones()))
                .build();
        replaceQuestions(e, req.getPreguntas());
        return toDto(examenRepo.save(e));
    }

    @Override
    @Transactional
    public ExamenResponse actualizar(Long cursoId, Long examenId, CrearExamenRequest req) {
        Examen e = findOrThrow(cursoId, examenId);
        if (req.getTitulo() != null) e.setTitulo(req.getTitulo());
        if (req.getDescripcion() != null) e.setDescripcion(req.getDescripcion());
        if (req.getDuracionMinutos() != null) e.setDuracionMinutos(req.getDuracionMinutos());
        if (req.getIntentosMaximos() != null) e.setIntentosMaximos(req.getIntentosMaximos());
        if (req.getNotaMinimaPropia() != null) e.setNotaMinimaPropia(req.getNotaMinimaPropia());
        if (req.getShufflePreguntas() != null) e.setShufflePreguntas(req.getShufflePreguntas());
        if (req.getShuffleOpciones() != null) e.setShuffleOpciones(req.getShuffleOpciones());

        if (req.getPreguntas() != null) {
            // Reemplazo total: orphanRemoval limpia las viejas
            e.getPreguntas().clear();
            replaceQuestions(e, req.getPreguntas());
        }
        return toDto(examenRepo.save(e));
    }

    @Override
    @Transactional
    public void borrar(Long cursoId, Long examenId) {
        examenRepo.delete(findOrThrow(cursoId, examenId));
    }

    /* helpers */

    private void validarCurso(Long cursoId) {
        if (!cursoRepo.existsById(cursoId)) {
            throw new CursosApiException(ServerApiStatusCode.CURSO_NO_ENCONTRADO);
        }
    }

    private Examen findOrThrow(Long cursoId, Long examenId) {
        Examen e = examenRepo.findById(examenId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.EXAMEN_NO_ENCONTRADO));
        if (!cursoId.equals(e.getCursoId())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El examen no pertenece al curso");
        }
        return e;
    }

    private void replaceQuestions(Examen e, List<PreguntaRequest> nuevas) {
        if (nuevas == null || nuevas.isEmpty()) return;
        for (int i = 0; i < nuevas.size(); i++) {
            PreguntaRequest pr = nuevas.get(i);
            PreguntaExamen p = PreguntaExamen.builder()
                    .examen(e)
                    .enunciado(pr.getEnunciado())
                    .tipo(pr.getTipo())
                    .orden(pr.getOrden() != null ? pr.getOrden() : i)
                    .puntos(pr.getPuntos() != null ? pr.getPuntos() : BigDecimal.ONE)
                    .respuestaEsperada(pr.getRespuestaEsperada())
                    .opciones(new ArrayList<>())
                    .build();

            if (pr.getOpciones() != null) {
                for (int j = 0; j < pr.getOpciones().size(); j++) {
                    OpcionRequest or = pr.getOpciones().get(j);
                    p.getOpciones().add(OpcionPregunta.builder()
                            .pregunta(p)
                            .texto(or.getTexto())
                            .correcta(Boolean.TRUE.equals(or.getCorrecta()))
                            .orden(or.getOrden() != null ? or.getOrden() : j)
                            .build());
                }
            }
            e.getPreguntas().add(p);
        }
    }

    private ExamenResponse toDto(Examen e) {
        List<PreguntaResponse> preguntas = e.getPreguntas() == null ? List.of()
                : e.getPreguntas().stream()
                .sorted(Comparator.comparingInt(PreguntaExamen::getOrden))
                .map(this::toPreguntaDto)
                .toList();
        return ExamenResponse.builder()
                .id(e.getId())
                .cursoId(e.getCursoId())
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .duracionMinutos(e.getDuracionMinutos())
                .intentosMaximos(e.getIntentosMaximos())
                .notaMinimaPropia(e.getNotaMinimaPropia())
                .shufflePreguntas(e.getShufflePreguntas())
                .shuffleOpciones(e.getShuffleOpciones())
                .preguntas(preguntas)
                .build();
    }

    private PreguntaResponse toPreguntaDto(PreguntaExamen p) {
        List<OpcionResponse> opc = p.getOpciones() == null ? List.of()
                : p.getOpciones().stream()
                .sorted(Comparator.comparingInt(OpcionPregunta::getOrden))
                .map(o -> OpcionResponse.builder()
                        .id(o.getId())
                        .preguntaId(p.getId())
                        .texto(o.getTexto())
                        .correcta(o.getCorrecta())
                        .orden(o.getOrden())
                        .build())
                .toList();
        return PreguntaResponse.builder()
                .id(p.getId())
                .examenId(p.getExamen() != null ? p.getExamen().getId() : null)
                .enunciado(p.getEnunciado())
                .tipo(p.getTipo())
                .orden(p.getOrden())
                .puntos(p.getPuntos())
                .respuestaEsperada(p.getRespuestaEsperada())
                .opciones(opc)
                .build();
    }
}
