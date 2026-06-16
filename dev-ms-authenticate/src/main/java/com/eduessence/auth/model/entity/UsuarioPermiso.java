package com.eduessence.auth.model.entity;

import com.eduessence.auth.model.enums.OverrideTipo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Override de permisos a nivel de usuario.
 *
 *   ALLOW → concede el permiso aunque ningún rol del usuario lo tenga.
 *   DENY  → revoca el permiso aunque algún rol del usuario lo tenga.
 *
 * El cálculo de permisos efectivos:
 *   efectivos = (∪ rol_permiso para roles del usuario)
 *             ∪ {permiso ∈ usuario_permiso WHERE tipo = ALLOW}
 *             \ {permiso ∈ usuario_permiso WHERE tipo = DENY}
 */
@Entity
@Table(name = "usuario_permiso", uniqueConstraints = @UniqueConstraint(
        name = "uk_up_usuario_permiso",
        columnNames = {"usuario_id", "permiso_id"}
), indexes = @Index(name = "idx_up_usuario", columnList = "usuario_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private Permiso permiso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OverrideTipo tipo;

    @Column(name = "asignado_por_usuario_id")
    private Long asignadoPorUsuarioId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
