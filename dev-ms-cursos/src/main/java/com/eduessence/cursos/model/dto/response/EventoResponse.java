package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.TipoSesion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sesión vista como "evento" — espejo plano que incluye contexto del curso
 * para que el cliente (landing o BO) no tenga que hacer joins extras.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponse {

    /** ID de la sesión virtual. */
    private Long id;

    /* Datos del evento */
    private TipoSesion tipo;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private Integer duracionMinutos;

    /** PRESENCIAL: lugar físico. */
    private String ubicacionTexto;

    /** VIRTUAL: URL HLS para alumnos matriculados. */
    private String playbackUrl;

    /** Grabación si ya terminó. */
    private String recordingUrl;

    /* Datos del curso al que pertenece */
    private Long cursoId;
    private String cursoNombre;
    private String cursoSlug;
    private String cursoLogoUrl;

    /* Cancelación */
    private Boolean cancelada;
    private String razonCancelacion;
}
