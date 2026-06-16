package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opcion_pregunta", indexes = @Index(name = "idx_opcion_pregunta", columnList = "pregunta_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionPregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private PreguntaExamen pregunta;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private Boolean correcta;

    @Column(nullable = false)
    private Integer orden;
}
