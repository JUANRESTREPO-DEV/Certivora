package com.eduessence.sendmail.model.entity;

import com.eduessence.sendmail.model.enums.EstadoEnvio;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Log de envíos para auditoría y debugging.
 */
@Entity
@Table(name = "email_envio", indexes = {
        @Index(name = "idx_envio_template", columnList = "template_id"),
        @Index(name = "idx_envio_fecha", columnList = "fecha_envio"),
        @Index(name = "idx_envio_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @Column(nullable = false, length = 200)
    private String destinatario;

    @Column(length = 200)
    private String nombreDestinatario;

    @Column(nullable = false, length = 300)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado;

    @Column(name = "variables_usadas", columnDefinition = "TEXT")
    private String variablesUsadas;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    void onCreate() {
        if (this.fechaEnvio == null) this.fechaEnvio = LocalDateTime.now();
    }
}
