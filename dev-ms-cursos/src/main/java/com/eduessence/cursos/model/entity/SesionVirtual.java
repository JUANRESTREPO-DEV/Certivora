package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.TipoSesion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sesión programada de clase en vivo. El {@code stream_session_id} apunta a
 * una sesión del microservicio dev-ms-streaming, que es quien maneja la
 * ingestion RTMP de OBS y el playback HLS.
 */
@Entity
@Table(name = "sesion_virtual", indexes = @Index(name = "idx_sv_curso", columnList = "curso_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionVirtual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    /** ID de la sesión en dev-ms-streaming (no FK, cross-MS). */
    @Column(name = "stream_session_id", length = 60)
    private String streamSessionId;

    /** URL HLS para reproducir (cache local; el verdadero estado lo da streaming). */
    @Column(name = "playback_url", length = 500)
    private String playbackUrl;

    /** Si el provider entregó una grabación al terminar. */
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    /** Clave secreta de stream para configurar en OBS (sensible — no exponer a alumnos). */
    @Column(name = "stream_key", length = 200)
    private String streamKey;

    /** RTMP endpoint donde OBS envía el video (sensible — sólo instructor/admin). */
    @Column(name = "ingest_url", length = 500)
    private String ingestUrl;

    /** VIRTUAL o PRESENCIAL. Una sesión presencial sólo usa qr_token + ubicacion. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoSesion tipo = TipoSesion.VIRTUAL;

    /** Sede / dirección donde se realiza la sesión presencial. */
    @Column(name = "ubicacion_texto", length = 500)
    private String ubicacionTexto;

    /** Token único para registro de asistencia por QR (PRESENCIAL). */
    @Column(name = "qr_token", length = 80, unique = true)
    private String qrToken;

    /** Flag de cancelación. Mantenemos la fila por trazabilidad y UI. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean cancelada = false;

    /** Motivo opcional mostrado a los matriculados al cancelar. */
    @Column(name = "razon_cancelacion", length = 500)
    private String razonCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;
}
