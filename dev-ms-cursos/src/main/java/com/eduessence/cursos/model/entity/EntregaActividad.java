package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrega_actividad", indexes = {
        @Index(name = "idx_ea_matricula", columnList = "matricula_id"),
        @Index(name = "idx_ea_actividad", columnList = "actividad_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false)
    private Long matriculaId;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "numero_entrega", nullable = false)
    private Integer numeroEntrega;

    @Column(name = "texto_entrega", columnDefinition = "TEXT")
    private String textoEntrega;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    /** Calificación 0-100. NULL = pendiente de calificar. */
    @Column(name = "calificacion", precision = 5, scale = 2)
    private BigDecimal calificacion;

    @Column(name = "calificado_por_usuario_id")
    private Long calificadoPorUsuarioId;

    @Column(name = "fecha_calificacion")
    private LocalDateTime fechaCalificacion;

    @Column(name = "comentario_calificador", columnDefinition = "TEXT")
    private String comentarioCalificador;

    @PrePersist
    void onCreate() {
        if (fechaEntrega == null) fechaEntrega = LocalDateTime.now();
    }
}
