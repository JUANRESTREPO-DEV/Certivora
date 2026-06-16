package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.TipoPregunta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pregunta_examen", indexes = @Index(name = "idx_pregunta_examen", columnList = "examen_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreguntaExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id", nullable = false)
    private Examen examen;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoPregunta tipo;

    @Column(nullable = false)
    private Integer orden;

    /** Peso de la pregunta en el examen (puntos). */
    @Column(name = "puntos", precision = 7, scale = 2, nullable = false)
    private BigDecimal puntos;

    /** Para TEXTO_CORTO: respuesta esperada (calificación manual igual). */
    @Column(name = "respuesta_esperada", columnDefinition = "TEXT")
    private String respuestaEsperada;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<OpcionPregunta> opciones = new ArrayList<>();
}
