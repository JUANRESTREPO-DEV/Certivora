package com.eduessence.pagos.model.entity;

import com.eduessence.pagos.model.enums.EstadoPago;
import com.eduessence.pagos.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pago", indexes = {
        @Index(name = "idx_pago_usuario", columnList = "usuario_id"),
        @Index(name = "idx_pago_curso", columnList = "curso_id"),
        @Index(name = "idx_pago_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "monto_original", nullable = false)
    private Integer montoOriginal;

    @Column(name = "monto_descuento", nullable = false)
    private Integer montoDescuento;

    /** Monto final a pagar (montoOriginal - montoDescuento). */
    @Column(name = "monto_cop", nullable = false)
    private Integer montoCop;

    @Column(length = 3, nullable = false)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPago estado;

    /** Llave Bre-B u otro identificador entregado al usuario para pagar. */
    @Column(length = 100)
    private String llave;

    /** Cuándo expira la reserva si no se confirma (si el curso tiene cupo limitado). */
    @Column(name = "reserva_expira")
    private LocalDateTime reservaExpira;

    @Column(name = "cupon_id")
    private Long cuponId;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Column(name = "referencia_pasarela", length = 120)
    private String referenciaPasarela;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "aprobado_por_usuario_id")
    private Long aprobadoPorUsuarioId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @PrePersist
    void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (moneda == null) moneda = "COP";
        if (montoDescuento == null) montoDescuento = 0;
    }
}
