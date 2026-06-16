package com.eduessence.auth.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Pantalla concreta del front. {@code path} debe coincidir con una ruta
 * Angular para que el sidebar la enrute directamente. {@code permisoEntrada}
 * (opcional) es el permiso mínimo que el usuario debe tener para que el
 * submódulo aparezca en su sidebar.
 */
@Entity
@Table(name = "submodulo", indexes = {
        @Index(name = "uk_submodulo_codigo", columnList = "codigo", unique = true),
        @Index(name = "uk_submodulo_path", columnList = "path", unique = true),
        @Index(name = "idx_submodulo_modulo", columnList = "modulo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submodulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @Column(nullable = false, length = 80, unique = true)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 120, unique = true)
    private String path;

    @Column(length = 60)
    private String icon;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean activo;

    /** Permiso mínimo para mostrar el submódulo en el sidebar (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permiso_entrada_id")
    private Permiso permisoEntrada;

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        if (orden == null) orden = 0;
    }
}
