package com.eduessence.pagos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cupon_uso", uniqueConstraints = @UniqueConstraint(
        name = "uk_cu_cupon_usuario",
        columnNames = {"cupon_id", "usuario_id"}
), indexes = @Index(name = "idx_cu_cupon", columnList = "cupon_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuponUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cupon_id", nullable = false)
    private Cupon cupon;

    @Column(name = "pago_id", nullable = false, unique = true)
    private Long pagoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "monto_descuento", nullable = false)
    private Integer montoDescuento;

    @Column(name = "fecha_uso", nullable = false)
    private LocalDateTime fechaUso;

    @PrePersist
    void onCreate() {
        if (fechaUso == null) fechaUso = LocalDateTime.now();
    }
}
