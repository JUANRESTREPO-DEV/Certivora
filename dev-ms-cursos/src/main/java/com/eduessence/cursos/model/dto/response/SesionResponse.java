package com.eduessence.cursos.model.dto.response;

import com.eduessence.cursos.model.enums.TipoSesion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionResponse {
    private Long id;
    private Long cursoId;
    private TipoSesion tipo;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private Integer duracionMinutos;

    /* VIRTUAL */
    private String streamSessionId;
    private String playbackUrl;
    private String recordingUrl;
    /** Clave secreta para configurar en OBS (sólo visible para instructor/admin). */
    private String streamKey;
    /** RTMP endpoint donde OBS envía el video. */
    private String ingestUrl;

    /* PRESENCIAL */
    private String ubicacionTexto;
    private String qrToken;
    /** URL pública del check-in (la app móvil escanea el QR y va aquí). */
    private String qrUrlCheckIn;

    /* Estado de cancelación */
    private Boolean cancelada;
    private String razonCancelacion;
    private LocalDateTime fechaCancelacion;
}
