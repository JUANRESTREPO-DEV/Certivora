package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "respuesta_intento", indexes = @Index(name = "idx_ri_intento", columnList = "intento_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intento_id", nullable = false)
    private IntentoExamen intento;

    @Column(name = "pregunta_id", nullable = false)
    private Long preguntaId;

    /** IDs de opciones seleccionadas (CSV) para SINGLE/MULTIPLE/VERDADERO_FALSO. */
    @Column(name = "opciones_seleccionadas", length = 500)
    private String opcionesSeleccionadas;

    /** Respuesta de texto para TEXTO_CORTO. */
    @Column(name = "respuesta_texto", columnDefinition = "TEXT")
    private String respuestaTexto;

    @Column(name = "puntos_obtenidos", precision = 7, scale = 2)
    private BigDecimal puntosObtenidos;

    @Column(nullable = false)
    private Boolean correcta;

    @PrePersist
    void onCreate() {
        if (correcta == null) correcta = false;
    }
}
