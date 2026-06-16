package com.eduessence.streaming.model.entity;

import com.eduessence.streaming.model.enums.EstadoStream;
import com.eduessence.streaming.model.enums.ProviderTipo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sesión de transmisión. Genera credenciales para OBS y URL de playback HLS.
 * El instructor configura en OBS:
 *   Server      = ingest_url
 *   Stream Key  = stream_key
 */
@Entity
@Table(name = "stream_session", indexes = {
        @Index(name = "uk_ss_stream_key", columnList = "stream_key", unique = true),
        @Index(name = "idx_ss_curso", columnList = "curso_id"),
        @Index(name = "idx_ss_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "sesion_virtual_id")
    private Long sesionVirtualId;

    @Column(name = "instructor_usuario_id", nullable = false)
    private Long instructorUsuarioId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderTipo provider;

    /** ID externo del recurso en el provider (channelArn de IVS, playback id de Mux, etc.). */
    @Column(name = "provider_resource_id", length = 200)
    private String providerResourceId;

    @Column(name = "ingest_url", nullable = false, length = 500)
    private String ingestUrl;

    @Column(name = "stream_key", nullable = false, length = 200, unique = true)
    private String streamKey;

    @Column(name = "playback_url", nullable = false, length = 500)
    private String playbackUrl;

    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoStream estado;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_estimada_minutos")
    private Integer duracionEstimadaMinutos;

    @Column(name = "duracion_real_segundos")
    private Long duracionRealSegundos;

    @Column(name = "viewers_peak")
    private Integer viewersPeak;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (estado == null) estado = EstadoStream.CREADO;
        if (viewersPeak == null) viewersPeak = 0;
    }
}
