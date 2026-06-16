package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Acumulado de presencia de un usuario en una sesión virtual.
 * Los heartbeats vienen desde dev-ms-streaming vía Feign al endpoint
 * {@code /internal/asistencia-virtual/heartbeat} y se agregan aquí.
 */
@Entity
@Table(name = "asistencia_virtual", uniqueConstraints = @UniqueConstraint(
        name = "uk_av_matricula_sesion",
        columnNames = {"matricula_id", "sesion_virtual_id"}
), indexes = {
        @Index(name = "idx_av_matricula", columnList = "matricula_id"),
        @Index(name = "idx_av_sesion", columnList = "sesion_virtual_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaVirtual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false)
    private Long matriculaId;

    @Column(name = "sesion_virtual_id", nullable = false)
    private Long sesionVirtualId;

    @Column(name = "minutos_conectado", nullable = false)
    private Integer minutosConectado;

    @Column(name = "porcentaje_presencia", precision = 5, scale = 2)
    private BigDecimal porcentajePresencia;

    @Column(name = "cumple_minimo", nullable = false)
    private Boolean cumpleMinimo;

    @Column(name = "primera_conexion")
    private LocalDateTime primeraConexion;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    @PrePersist
    void onCreate() {
        if (minutosConectado == null) minutosConectado = 0;
        if (cumpleMinimo == null) cumpleMinimo = false;
    }
}
