package com.eduessence.inbox.model.entity;

import com.eduessence.inbox.model.enums.TipoBuzon;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "buzon", indexes = {
        @Index(name = "idx_buzon_tipo", columnList = "tipo"),
        @Index(name = "idx_buzon_referencia", columnList = "tipo,referencia_id"),
        @Index(name = "idx_buzon_activo", columnList = "activo")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Buzon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, unique = true)
    private String direccion;

    @Column(name = "nombre_mostrar", length = 150)
    private String nombreMostrar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoBuzon tipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    /** JSON con direcciones externas a las que reenviamos: ["a@gmail.com","b@hotmail.com"] */
    @Column(name = "forward_externos", columnDefinition = "json")
    private String forwardExternosJson;

    /** Firma HTML/Texto que se anexa a las respuestas enviadas desde este buzón. */
    @Column(name = "firma", columnDefinition = "text")
    private String firma;

    @Column(name = "creado_por_usuario_id", nullable = false)
    private Long creadoPorUsuarioId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
