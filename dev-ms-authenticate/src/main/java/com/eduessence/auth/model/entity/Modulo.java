package com.eduessence.auth.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Grupo de alto nivel del sidebar (Cursos, Pagos, Configuración, etc.).
 * Catálogo administrado; no se mutan en runtime.
 */
@Entity
@Table(name = "modulo", indexes = @Index(name = "uk_modulo_codigo", columnList = "codigo", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 60)
    private String icon;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean activo;

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        if (orden == null) orden = 0;
    }
}
