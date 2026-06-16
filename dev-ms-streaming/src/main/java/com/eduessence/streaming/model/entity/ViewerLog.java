package com.eduessence.streaming.model.entity;

import com.eduessence.streaming.model.enums.EventoViewer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Eventos de un viewer durante la transmisión.
 *  - JOIN/LEAVE marcan los bordes de la sesión del viewer.
 *  - HEARTBEAT llega cada 30 s desde el player y sirve para calcular
 *    el tiempo real conectado (no se confía en la diferencia JOIN-LEAVE
 *    porque el viewer puede cerrar pestaña).
 */
@Entity
@Table(name = "viewer_log", indexes = {
        @Index(name = "idx_vl_session", columnList = "stream_session_id"),
        @Index(name = "idx_vl_usuario", columnList = "usuario_id"),
        @Index(name = "idx_vl_timestamp", columnList = "timestamp_evento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_session_id", nullable = false)
    private Long streamSessionId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "session_token", length = 80)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventoViewer evento;

    @Column(name = "timestamp_evento", nullable = false)
    private LocalDateTime timestampEvento;

    @Column(name = "segundos_acumulados")
    private Integer segundosAcumulados;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @PrePersist
    void onCreate() {
        if (timestampEvento == null) timestampEvento = LocalDateTime.now();
    }
}
