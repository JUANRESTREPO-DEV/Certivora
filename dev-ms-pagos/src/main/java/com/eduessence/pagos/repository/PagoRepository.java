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

    /**
     * Suma de {@code montoCop} de todos los pagos APROBADOS por curso.
     * Agrupado por {@code cursoId}. Usado en el reporte de matrículas.
     */
    @Query(value = """
            SELECT curso_id AS cursoId, COALESCE(SUM(monto_cop), 0) AS ingresos
            FROM pago
            WHERE estado = 'APROBADO'
            GROUP BY curso_id
            """, nativeQuery = true)
    List<Object[]> sumIngresosAprobadosPorCurso();
}
