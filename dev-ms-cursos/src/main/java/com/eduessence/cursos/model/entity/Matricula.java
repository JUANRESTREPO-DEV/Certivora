package com.eduessence.cursos.model.entity;

import com.eduessence.cursos.model.enums.EstadoMatricula;
import com.eduessence.cursos.model.enums.ModalidadTipo;
import com.eduessence.cursos.model.enums.TipoParticipante;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matricula", uniqueConstraints = @UniqueConstraint(
        name = "uk_matricula_usuario_curso",
        columnNames = {"usuario_id", "curso_id"}
), indexes = {
        @Index(name = "idx_matricula_usuario", columnList = "usuario_id"),
        @Index(name = "idx_matricula_curso", columnList = "curso_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoMatricula estado;

    /**
     * Tipo de participante. {@code ASISTENTE} es el default del flujo público;
     * {@code PONENTE / ORGANIZADOR / STAFF} solo se asignan desde admin.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoParticipante tipo = TipoParticipante.ASISTENTE;

    /** ID de la fila en {@code pago} de dev-ms-pagos. {@code null} si es gratis o cortesía. */
    @Column(name = "pago_id")
    private Long pagoId;

    /**
     * Precio COP congelado al inscribirse. Sobrevive a cambios posteriores
     * del precio del curso. {@code null} si es cortesía o gratis.
     */
    @Column(name = "precio_cop_pagado", precision = 12, scale = 0)
    private BigDecimal precioCopPagado;

    /**
     * Token único para la escarapela pública del asistente. Se genera al
     * inscribirse en un curso que ofrece PRESENCIAL, se comparte por URL en
     * {@code /public/escarapela/{token}}. {@code null} para matrículas que
     * no son PRESENCIAL o cursos sin plantilla de escarapela.
     */
    @Column(name = "escarapela_token", length = 64, unique = true)
    private String escarapelaToken;

    /** Hasta cuándo se mantiene el cupo reservado mientras se confirma el pago. */
    @Column(name = "reserva_expira")
    private LocalDateTime reservaExpira;

    /** Posición FIFO en la lista de espera. {@code null} fuera de {@code EN_ESPERA}. */
    @Column(name = "posicion_espera")
    private Integer posicionEspera;

    /** Inscripción de cortesía (sin pago) creada por admin. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean cortesia = false;

    @Column(name = "cancelado_motivo", length = 500)
    private String canceladoMotivo;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    /** Usuario que ejecutó la cancelación (puede ser el mismo alumno o un admin). */
    @Column(name = "cancelado_por_usuario_id")
    private Long canceladoPorUsuarioId;

    /** Modalidades que el usuario está cursando (puede ser un subset de las del curso). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "matricula_modalidad",
            joinColumns = @JoinColumn(name = "matricula_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_mm_matricula_tipo", columnNames = {"matricula_id","tipo"}))
    @Column(name = "tipo", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<ModalidadTipo> modalidades = new HashSet<>();

    @Column(name = "progreso_pct", precision = 5, scale = 2)
    private BigDecimal progresoPct;

    @Column(name = "nota_final", precision = 5, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "certificado_emitido", nullable = false)
    private Boolean certificadoEmitido;

    @PrePersist
    void onCreate() {
        if (estado == null) estado = EstadoMatricula.PENDIENTE_PAGO;
        if (tipo == null) tipo = TipoParticipante.ASISTENTE;
        if (cortesia == null) cortesia = false;
        if (fechaInscripcion == null) fechaInscripcion = LocalDateTime.now();
        if (progresoPct == null) progresoPct = BigDecimal.ZERO;
        if (certificadoEmitido == null) certificadoEmitido = false;
    }

    /** Conveniencia: ¿la inscripción consume cupo activamente? */
    public boolean ocupaCupo() {
        return estado == EstadoMatricula.PENDIENTE_PAGO
                || estado == EstadoMatricula.ACTIVA
                || estado == EstadoMatricula.APROBADA
                || estado == EstadoMatricula.FINALIZADA;
    }
}
