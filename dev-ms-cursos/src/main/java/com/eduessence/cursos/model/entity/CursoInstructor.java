package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "curso_instructor", uniqueConstraints = @UniqueConstraint(
        name = "uk_ci_curso_usuario", columnNames = {"curso_id", "usuario_id"}),
        indexes = {
                @Index(name = "idx_ci_curso", columnList = "curso_id"),
                @Index(name = "idx_ci_usuario", columnList = "usuario_id")
        })
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CursoInstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** True para el instructor principal (uno solo por curso). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean principal = false;
}
