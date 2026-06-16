package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seccion", indexes = @Index(name = "idx_seccion_curso", columnList = "curso_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer orden;

    /**
     * Si true, no se puede avanzar a la siguiente sección hasta que todas
     * las lecciones {@code bloqueante=true} de esta sección estén completadas.
     */
    @Column(nullable = false)
    private Boolean bloqueante;

    @OneToMany(mappedBy = "seccion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<Leccion> lecciones = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (bloqueante == null) bloqueante = false;
    }
}
