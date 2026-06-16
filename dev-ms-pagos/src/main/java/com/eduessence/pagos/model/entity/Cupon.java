package com.eduessence.pagos.model.entity;

import com.eduessence.pagos.model.enums.AlcanceCupon;
import com.eduessence.pagos.model.enums.TipoDescuento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cupon", indexes = {
        @Index(name = "uk_cupon_codigo", columnList = "codigo", unique = true),
        @Index(name = "idx_cupon_activo", columnList = "activo, fecha_inicio, fecha_fin")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false, length = 20)
    private TipoDescuento tipoDescuento;

    @Column(nullable = false)
    private Integer valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlcanceCupon alcance;

    @Column(name = "usuario_asignado_id")
    private Long usuarioAsignadoId;

    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    @Column(name = "usos_actuales", nullable = false)
    private Integer usosActuales;

    /** Si NULL → aplica a cualquier curso. */
    @Column(name = "curso_id")
    private Long cursoId;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "creado_por_usuario_id", nullable = false)
    private Long creadoPorUsuarioId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        if (activo == null) activo = true;
        if (usosActuales == null) usosActuales = 0;
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (codigo != null) codigo = codigo.toUpperCase();
    }
}
