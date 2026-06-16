package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.ModalidadTipo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "curso_modalidad", uniqueConstraints = @UniqueConstraint(
        name = "uk_curso_modalidad",
        columnNames = {"curso_id", "tipo"}
), indexes = @Index(name = "idx_cm_curso", columnList = "curso_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoModalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModalidadTipo tipo;

    /** Solo para PRESENCIAL: lugar físico. */
    @Column(length = 200)
    private String sede;

    /** Solo para PRESENCIAL/VIRTUAL_LIVE: cupo específico de la modalidad. */
    @Column(name = "cupo_modalidad")
    private Integer cupoModalidad;

    @Column(nullable = false)
    private Boolean activo;

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
    }
}
