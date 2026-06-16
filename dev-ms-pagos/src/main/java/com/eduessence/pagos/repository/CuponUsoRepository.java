package com.eduessence.pagos.repository;

import com.eduessence.pagos.model.entity.CuponUso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuponUsoRepository extends JpaRepository<CuponUso, Long> {
    Optional<CuponUso> findByCupon_IdAndUsuarioId(Long cuponId, Long usuarioId);
    Optional<CuponUso> findByPagoId(Long pagoId);

    /** Histórico de un cupón ordenado por fecha de uso descendente. */
    List<CuponUso> findByCupon_IdOrderByFechaUsoDesc(Long cuponId);
}
