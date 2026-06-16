package com.eduessence.cursos.service.impl;

import com.eduessence.cursos.exception.CursosApiException;
import com.eduessence.cursos.exception.ServerApiStatusCode;
import com.eduessence.cursos.model.dto.request.ActualizarProgresoRequest;
import com.eduessence.cursos.model.dto.request.GuardarRespuestaRequest;
import com.eduessence.cursos.model.dto.response.IntentoExamenResponse;
import com.eduessence.cursos.model.dto.response.IntentoExamenResponse.OpcionIntentoDTO;
import com.eduessence.cursos.model.dto.response.IntentoExamenResponse.PreguntaIntentoDTO;
import com.eduessence.cursos.model.dto.response.IntentoExamenResponse.RespuestaIntentoDTO;
import com.eduessence.cursos.model.entity.Examen;
import com.eduessence.cursos.model.entity.IntentoExamen;
import com.eduessence.cursos.model.entity.Leccion;
import com.eduessence.cursos.model.entity.OpcionPregunta;
import com.eduessence.cursos.model.entity.PreguntaExamen;
import com.eduessence.cursos.model.entity.RespuestaIntento;
import com.eduessence.cursos.model.enums.TipoPregunta;
import com.eduessence.cursos.repository.ExamenRepository;
import com.eduessence.cursos.repository.IntentoExamenRepository;
import com.eduessence.cursos.repository.LeccionRepository;
import com.eduessence.cursos.repository.RespuestaIntentoRepository;
import com.eduessence.cursos.service.IntentoExamenService;
import com.eduessence.cursos.service.ProgresoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentoExamenServiceImpl implements IntentoExamenService {

    private final IntentoExamenRepository intentoRepository;
    private final RespuestaIntentoRepository respuestaRepository;
    private final ExamenRepository examenRepository;
    private final LeccionRepository leccionRepository;
    private final ProgresoService progresoService;

    /* ──────────────────────────────────────────────────────────────────
     * INICIAR
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public IntentoExamenResponse iniciar(Long matriculaId, Long examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.EXAMEN_NO_ENCONTRADO));

        long usados = intentoRepository.countByMatriculaIdAndExamenId(matriculaId, examenId);
        if (usados >= examen.getIntentosMaximos()) {
            throw new CursosApiException(ServerApiStatusCode.INTENTOS_AGOTADOS);
        }

        // Si ya hay un intento previo SIN finalizar, lo devolvemos (idempotencia).
        IntentoExamen activo = intentoRepository
                .findAllByMatriculaIdAndExamenIdOrderByNumeroIntentoDesc(matriculaId, examenId)
                .stream()
                .filter(i -> !Boolean.TRUE.equals(i.getFinalizado()))
                .findFirst()
                .orElse(null);
        if (activo != null) {
            return toResponse(activo, examen);
        }

        IntentoExamen nuevo = IntentoExamen.builder()
                .matriculaId(matriculaId)
                .examenId(examenId)
                .numeroIntento((int) usados + 1)
                .finalizado(false)
                .requiereCalificacionManual(false)
                .build();
        nuevo = intentoRepository.save(nuevo);
        log.info("Intento {} iniciado · matricula={} · examen={}",
                nuevo.getId(), matriculaId, examenId);
        return toResponse(nuevo, examen);
    }

    /* ──────────────────────────────────────────────────────────────────
     * GUARDAR RESPUESTA
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public IntentoExamenResponse guardarRespuesta(Long intentoId, GuardarRespuestaRequest req) {
        IntentoExamen intento = findIntento(intentoId);
        if (Boolean.TRUE.equals(intento.getFinalizado())) {
            throw new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                    "El intento ya está finalizado");
        }

        RespuestaIntento r = respuestaRepository
                .findByIntentoIdAndPreguntaId(intentoId, req.getPreguntaId())
                .orElseGet(() -> RespuestaIntento.builder()
                        .intento(intento)
                        .preguntaId(req.getPreguntaId())
                        .correcta(false)
                        .build());

        r.setOpcionesSeleccionadas(serializarIds(req.getOpcionesSeleccionadas()));
        r.setRespuestaTexto(req.getRespuestaTexto());
        respuestaRepository.save(r);

        Examen examen = examenRepository.findById(intento.getExamenId()).orElseThrow();
        return toResponse(intento, examen);
    }

    /* ──────────────────────────────────────────────────────────────────
     * FINALIZAR
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional
    public IntentoExamenResponse finalizar(Long intentoId) {
        IntentoExamen intento = findIntento(intentoId);
        if (Boolean.TRUE.equals(intento.getFinalizado())) {
            Examen examen = examenRepository.findById(intento.getExamenId()).orElseThrow();
            return toResponse(intento, examen);
        }

        Examen examen = examenRepository.findById(intento.getExamenId())
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.EXAMEN_NO_ENCONTRADO));

        // Calificar cada pregunta
        BigDecimal puntosTotalesObtenidos = BigDecimal.ZERO;
        BigDecimal puntosTotalesPosibles = BigDecimal.ZERO;
        boolean requiereManual = false;

        List<RespuestaIntento> respuestas = respuestaRepository.findAllByIntentoId(intentoId);
        for (PreguntaExamen p : examen.getPreguntas()) {
            puntosTotalesPosibles = puntosTotalesPosibles.add(p.getPuntos());

            RespuestaIntento r = respuestas.stream()
                    .filter(x -> x.getPreguntaId().equals(p.getId()))
                    .findFirst()
                    .orElse(null);
            if (r == null) continue; // sin responder → 0 puntos

            CorreccionResult res = calificarPregunta(p, r);
            r.setCorrecta(res.correcta);
            r.setPuntosObtenidos(res.puntosObtenidos);
            respuestaRepository.save(r);
            if (res.requiereManual) requiereManual = true;
            puntosTotalesObtenidos = puntosTotalesObtenidos.add(res.puntosObtenidos);
        }

        BigDecimal puntaje = puntosTotalesPosibles.compareTo(BigDecimal.ZERO) > 0
                ? puntosTotalesObtenidos
                    .multiply(new BigDecimal(100))
                    .divide(puntosTotalesPosibles, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        intento.setPuntaje(puntaje);
        intento.setFinalizado(true);
        intento.setRequiereCalificacionManual(requiereManual);
        intento.setFechaFin(LocalDateTime.now());
        intentoRepository.save(intento);
        log.info("Intento {} finalizado · puntaje={} · requiereManual={}",
                intentoId, puntaje, requiereManual);

        // Marca como COMPLETADA cualquier lección vinculada a este examen
        // para que cuente en el progresoPct de la matrícula. Idempotente:
        // ProgresoService.actualizar() ignora si ya está COMPLETADA.
        marcarLeccionesVinculadasCompletadas(intento.getMatriculaId(), intento.getExamenId());

        return toResponse(intento, examen);
    }

    /**
     * Si el examen está vinculado a una o más lecciones (lección tipo EXAMEN o
     * lección VIDEO con quiz al final), las marca como COMPLETADA en el
     * progreso del alumno. Cada llamada recalcula el % global de la matrícula.
     */
    private void marcarLeccionesVinculadasCompletadas(Long matriculaId, Long examenId) {
        List<Leccion> lecciones = leccionRepository.findAllByExamenId(examenId);
        if (lecciones.isEmpty()) return;

        ActualizarProgresoRequest req = new ActualizarProgresoRequest();
        req.setCompletada(true);

        for (Leccion l : lecciones) {
            try {
                progresoService.actualizar(matriculaId, l.getId(), req);
                log.debug("Lección {} marcada como completada por examen {}",
                        l.getId(), examenId);
            } catch (Exception ex) {
                // No bloqueamos la finalización del intento si el progreso falla
                log.warn("No se pudo actualizar progreso de lección {}: {}",
                        l.getId(), ex.getMessage());
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────
     * CONSULTAS
     * ────────────────────────────────────────────────────────────────── */

    @Override
    @Transactional(readOnly = true)
    public IntentoExamenResponse obtener(Long intentoId) {
        IntentoExamen intento = findIntento(intentoId);
        Examen examen = examenRepository.findById(intento.getExamenId())
                .orElseThrow(() -> new CursosApiException(ServerApiStatusCode.EXAMEN_NO_ENCONTRADO));
        return toResponse(intento, examen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntentoExamenResponse> listarPorMatriculaYExamen(Long matriculaId, Long examenId) {
        Examen examen = examenRepository.findById(examenId).orElse(null);
        return intentoRepository
                .findAllByMatriculaIdAndExamenIdOrderByNumeroIntentoDesc(matriculaId, examenId)
                .stream()
                .map(i -> toResponse(i, examen))
                .toList();
    }

    /* ──────────────────────────────────────────────────────────────────
     * CALIFICACIÓN
     * ────────────────────────────────────────────────────────────────── */

    private CorreccionResult calificarPregunta(PreguntaExamen p, RespuestaIntento r) {
        if (p.getTipo() == TipoPregunta.TEXTO_CORTO) {
            // Por ahora marcamos como requiere manual. Si la respuesta esperada
            // coincide ignorando casing/trim, damos los puntos automáticamente.
            String esperada = p.getRespuestaEsperada() == null
                    ? "" : p.getRespuestaEsperada().trim().toLowerCase();
            String dada = r.getRespuestaTexto() == null
                    ? "" : r.getRespuestaTexto().trim().toLowerCase();
            if (!esperada.isEmpty() && esperada.equals(dada)) {
                return new CorreccionResult(true, p.getPuntos(), false);
            }
            return new CorreccionResult(false, BigDecimal.ZERO, true);
        }

        Set<Long> seleccionadas = parseIds(r.getOpcionesSeleccionadas());
        Set<Long> correctas = p.getOpciones().stream()
                .filter(o -> Boolean.TRUE.equals(o.getCorrecta()))
                .map(OpcionPregunta::getId)
                .collect(Collectors.toSet());

        boolean acierto = !correctas.isEmpty() && correctas.equals(seleccionadas);
        return new CorreccionResult(
                acierto,
                acierto ? p.getPuntos() : BigDecimal.ZERO,
                false);
    }

    private record CorreccionResult(boolean correcta, BigDecimal puntosObtenidos, boolean requiereManual) {}

    /* ──────────────────────────────────────────────────────────────────
     * MAPPERS
     * ────────────────────────────────────────────────────────────────── */

    private IntentoExamenResponse toResponse(IntentoExamen intento, Examen examen) {
        boolean finalizado = Boolean.TRUE.equals(intento.getFinalizado());

        BigDecimal notaMin = examen.getNotaMinimaPropia();
        Boolean aprobado = null;
        if (finalizado && notaMin != null && intento.getPuntaje() != null) {
            aprobado = intento.getPuntaje().compareTo(notaMin) >= 0;
        }

        return IntentoExamenResponse.builder()
                .id(intento.getId())
                .matriculaId(intento.getMatriculaId())
                .examenId(intento.getExamenId())
                .examenTitulo(examen.getTitulo())
                .duracionMinutos(examen.getDuracionMinutos())
                .numeroIntento(intento.getNumeroIntento())
                .intentosMaximos(examen.getIntentosMaximos())
                .fechaInicio(intento.getFechaInicio())
                .fechaFin(intento.getFechaFin())
                .puntaje(intento.getPuntaje())
                .notaMinima(notaMin)
                .finalizado(finalizado)
                .aprobado(aprobado)
                .requiereCalificacionManual(intento.getRequiereCalificacionManual())
                .preguntas(mapPreguntas(examen, finalizado))
                .respuestas(mapRespuestas(intento.getId(), finalizado))
                .build();
    }

    private List<PreguntaIntentoDTO> mapPreguntas(Examen examen, boolean finalizado) {
        return examen.getPreguntas().stream()
                .sorted(Comparator.comparingInt(PreguntaExamen::getOrden))
                .map(p -> PreguntaIntentoDTO.builder()
                        .id(p.getId())
                        .enunciado(p.getEnunciado())
                        .tipo(p.getTipo())
                        .orden(p.getOrden())
                        .puntos(p.getPuntos())
                        .opciones(p.getOpciones().stream()
                                .sorted(Comparator.comparingInt(OpcionPregunta::getOrden))
                                .map(o -> OpcionIntentoDTO.builder()
                                        .id(o.getId())
                                        .texto(o.getTexto())
                                        .orden(o.getOrden())
                                        // Solo expone "correcta" cuando el intento ya finalizó
                                        .correcta(finalizado ? o.getCorrecta() : null)
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private List<RespuestaIntentoDTO> mapRespuestas(Long intentoId, boolean finalizado) {
        return respuestaRepository.findAllByIntentoId(intentoId).stream()
                .map(r -> RespuestaIntentoDTO.builder()
                        .preguntaId(r.getPreguntaId())
                        .opcionesSeleccionadas(new java.util.ArrayList<>(parseIds(r.getOpcionesSeleccionadas())))
                        .respuestaTexto(r.getRespuestaTexto())
                        .puntosObtenidos(finalizado ? r.getPuntosObtenidos() : null)
                        .correcta(finalizado ? r.getCorrecta() : null)
                        .build())
                .toList();
    }

    /* ──────────────────────────────────────────────────────────────────
     * HELPERS
     * ────────────────────────────────────────────────────────────────── */

    private IntentoExamen findIntento(Long id) {
        return intentoRepository.findById(id).orElseThrow(() ->
                new CursosApiException(ServerApiStatusCode.DATOS_INVALIDOS,
                        "Intento " + id + " no existe"));
    }

    private String serializarIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Set<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return new HashSet<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}
