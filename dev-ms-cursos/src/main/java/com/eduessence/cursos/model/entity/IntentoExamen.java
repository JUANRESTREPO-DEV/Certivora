package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "intento_examen", indexes = {
        @Index(name = "idx_ie_matricula", columnList = "matricula_id"),
        @Index(name = "idx_ie_examen", columnList = "examen_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false)
    private Long matriculaId;

    @Column(name = "examen_id", nullable = false)
    private Long examenId;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    /** Puntaje obtenido (0-100). */
    @Column(name = "puntaje", precision = 5, scale = 2)
    private BigDecimal puntaje;

    @Column(nullable = false)
    private Boolean finalizado;

    @Column(name = "requiere_calificacion_manual", nullable = false)
    private Boolean requiereCalificacionManual;

    @OneToMany(mappedBy = "intento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RespuestaIntento> respuestas = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
        if (finalizado == null) finalizado = false;
        if (requiereCalificacionManual == null) requiereCalificacionManual = false;
    }
}
