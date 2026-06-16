package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "examen", indexes = @Index(name = "idx_examen_curso", columnList = "curso_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    /** Duración máxima del intento, en minutos. */
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    /** Máximo de intentos permitidos por matrícula. */
    @Column(name = "intentos_maximos", nullable = false)
    private Integer intentosMaximos;

    /** Nota mínima para aprobar este examen (0-100). Si NULL → cae al curso. */
    @Column(name = "nota_minima_propia", precision = 5, scale = 2)
    private BigDecimal notaMinimaPropia;

    @Column(name = "shuffle_preguntas", nullable = false)
    private Boolean shufflePreguntas;

    @Column(name = "shuffle_opciones", nullable = false)
    private Boolean shuffleOpciones;

    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<PreguntaExamen> preguntas = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (intentosMaximos == null) intentosMaximos = 1;
        if (shufflePreguntas == null) shufflePreguntas = false;
        if (shuffleOpciones == null) shuffleOpciones = false;
    }
}
