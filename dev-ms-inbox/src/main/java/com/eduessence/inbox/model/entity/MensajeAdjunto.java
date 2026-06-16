package com.eduessence.inbox.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mensaje_adjunto", indexes = {
        @Index(name = "idx_adj_mensaje", columnList = "mensaje_id")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class MensajeAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @Column(length = 255, nullable = false)
    private String nombre;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "s3_key", length = 500, nullable = false)
    private String s3Key;
}
