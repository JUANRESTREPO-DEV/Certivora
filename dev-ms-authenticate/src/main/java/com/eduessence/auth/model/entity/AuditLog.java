package com.eduessence.auth.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_usuario", columnList = "usuario_id"),
        @Index(name = "idx_audit_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(length = 20)
    private String resultado;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(length = 120)
    private String recurso;

    @PrePersist
    void onCreate() {
        if (this.fecha == null) this.fecha = LocalDateTime.now();
    }
}
