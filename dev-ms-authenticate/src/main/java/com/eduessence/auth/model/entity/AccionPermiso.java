package com.eduessence.auth.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Junction acción ↔ permiso.
 *
 *   required = true  → el permiso es obligatorio para que la acción funcione.
 *                      Se usa para computar GRANTED / PARTIAL / DENIED.
 *   required = false → opcional (mejora la UX pero no la condiciona).
 *                      Solo se considera en bundles de admin.
 */
@Entity
@Table(name = "accion_permiso", uniqueConstraints = @UniqueConstraint(
        name = "uk_accion_permiso",
        columnNames = {"accion_id", "permiso_id"}
), indexes = {
        @Index(name = "idx_ap_accion", columnList = "accion_id"),
        @Index(name = "idx_ap_permiso", columnList = "permiso_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccionPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accion_id", nullable = false)
    private Accion accion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private Permiso permiso;

    @Column(nullable = false)
    private Boolean required;

    @PrePersist
    void onCreate() {
        if (required == null) required = true;
    }
}
