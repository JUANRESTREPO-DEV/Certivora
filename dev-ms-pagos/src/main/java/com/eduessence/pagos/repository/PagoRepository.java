package com.eduessence.pagos.repository;

import com.eduessence.pagos.model.entity.Pago;
import com.eduessence.pagos.model.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findAllByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
    long countByCursoIdAndEstadoIn(Long cursoId, List<EstadoPago> estados);
    List<Pago> findAllByEstadoAndReservaExpiraBefore(EstadoPago estado, LocalDateTime momento);
    List<Pago> findAllByEstadoOrderByFechaCreacionAsc(EstadoPago estado);

    /**
     * Pendientes en cualquiera de los estados — útil para la bandeja admin.
     *
     * <p>Native query para evitar el bug clásico de Spring Data JPA con
     * enums + {@code IN}. Por seguridad recibe {@code List<String>}
     * (nombres del enum) — el caller hace {@code EstadoPago.name()} antes
     * de invocar.</p>
     */
    @Query(value = """
            SELECT * FROM pago
            WHERE estado IN (:estados)
            ORDER BY fecha_creacion ASC
            """, nativeQuery = true)
    List<Pago> findAllByEstadoInOrderByFechaCreacionAsc(@Param("estados") List<String> estados);
}
