package com.eduessence.cursos.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de check-in de un matriculado a una sesión PRESENCIAL. Se genera
 * al escanear el QR de la escarapela o al marcar manualmente desde el
 * panel de check-in del operador.
 *
 * <p>Unique {@code (matricula_id, sesion_virtual_id)} evita doble check-in.
 * Si el asistente vuelve a escanear el mismo QR, el servicio devuelve
 * {@code YA_REGISTRADO} y no crea fila nueva.</p>
 */
@Entity
@Table(name = "asistencia_presencial", uniqueConstraints = @UniqueConstraint(
        name = "uk_asis_pres_mat_ses",
        columnNames = {"matricula_id", "sesion_virtual_id"}
), indexes = {
        @Index(name = "idx_asis_pres_sesion", columnList = "sesion_virtual_id"),
        @Index(name = "idx_asis_pres_matricula", columnList = "matricula_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaPresencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matricula_id", nullable = false)
    private Long matriculaId;

    @Column(name = "sesion_virtual_id", nullable = false)
    private Long sesionVirtualId;

    @Column(name = "fecha_checkin", nullable = false)
    private LocalDateTime fechaCheckin;

    /** Usuario admin/operador que registró el check-in. {@code null} si es auto-scan. */
    @Column(name = "operador_usuario_id")
    private Long operadorUsuarioId;

    @PrePersist
    void onCreate() {
        if (fechaCheckin == null) fechaCheckin = LocalDateTime.now();
    }
}
