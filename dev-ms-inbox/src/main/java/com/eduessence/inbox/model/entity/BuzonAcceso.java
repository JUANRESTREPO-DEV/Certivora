package com.eduessence.inbox.model.entity;

import com.eduessence.inbox.model.enums.RolBuzon;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "buzon_acceso",
        uniqueConstraints = @UniqueConstraint(name = "uk_ba_buzon_usuario", columnNames = {"buzon_id", "usuario_id"}),
        indexes = {
                @Index(name = "idx_ba_usuario", columnList = "usuario_id")
        })
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BuzonAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buzon_id", nullable = false)
    private Buzon buzon;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RolBuzon rol = RolBuzon.LECTOR;

    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
