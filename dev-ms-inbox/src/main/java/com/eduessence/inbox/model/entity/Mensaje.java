package com.eduessence.inbox.model.entity;

import com.eduessence.inbox.model.enums.Carpeta;
import com.eduessence.inbox.model.enums.EstadoMensaje;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje", indexes = {
        @Index(name = "idx_msg_buzon_carpeta", columnList = "buzon_id,carpeta,fecha_recibido"),
        @Index(name = "idx_msg_thread", columnList = "thread_id"),
        @Index(name = "idx_msg_message_id", columnList = "message_id")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buzon_id", nullable = false)
    private Buzon buzon;

    @Column(name = "message_id", length = 255, unique = true)
    private String messageId;

    @Column(name = "thread_id", length = 255)
    private String threadId;

    @Column(name = "in_reply_to", length = 255)
    private String inReplyTo;

    @Column(name = "remitente_email", length = 200, nullable = false)
    private String remitenteEmail;

    @Column(name = "remitente_nombre", length = 200)
    private String remitenteNombre;

    /** JSON array de destinatarios To/Cc/Bcc. */
    @Column(name = "destinatarios", columnDefinition = "json")
    private String destinatariosJson;

    @Column(length = 500)
    private String asunto;

    @Column(name = "snippet", length = 300)
    private String snippet;

    @Column(name = "cuerpo_texto", columnDefinition = "mediumtext")
    private String cuerpoTexto;

    @Column(name = "cuerpo_html", columnDefinition = "mediumtext")
    private String cuerpoHtml;

    /** S3 key del .eml crudo (para reproceso o auditoría). */
    @Column(name = "s3_eml_key", length = 500)
    private String s3EmlKey;

    @Column(name = "fecha_recibido", nullable = false)
    private LocalDateTime fechaRecibido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMensaje estado = EstadoMensaje.RECIBIDO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Carpeta carpeta = Carpeta.INBOX;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "destacado", nullable = false)
    @Builder.Default
    private Boolean destacado = false;

    @Column(name = "tiene_adjuntos", nullable = false)
    @Builder.Default
    private Boolean tieneAdjuntos = false;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    /** Si es un mensaje saliente, quién lo envió. */
    @Column(name = "enviado_por_usuario_id")
    private Long enviadoPorUsuarioId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
