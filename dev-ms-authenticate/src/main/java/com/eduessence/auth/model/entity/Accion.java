package com.eduessence.auth.model.entity;

import com.eduessence.auth.model.enums.AccionTipo;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Bundle funcional dentro de un submódulo. Una acción agrupa N permisos
 * atómicos vía {@link AccionPermiso}. El admin SaaS gestiona ACCIONES
 * (más intuitivo); el cálculo de estado computa GRANTED / PARTIAL / DENIED
 * a partir de los permisos efectivos del usuario.
 */
@Entity
@Table(name = "accion", indexes = {
        @Index(name = "uk_accion_codigo", columnList = "codigo", unique = true),
        @Index(name = "idx_accion_submodulo", columnList = "submodulo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submodulo_id", nullable = false)
    private Submodulo submodulo;

    @Column(nullable = false, length = 100, unique = true)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(length = 300)
    private String descripcion;

    @Column(length = 60)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionTipo tipo;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean activo;

    @OneToMany(mappedBy = "accion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AccionPermiso> permisos = new HashSet<>();

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        if (orden == null) orden = 0;
        if (tipo == null) tipo = AccionTipo.BUTTON;
    }
}
