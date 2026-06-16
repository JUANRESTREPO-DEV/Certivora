package com.eduessence.pagos.repository;

import com.eduessence.pagos.model.entity.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuponRepository extends JpaRepository<Cupon, Long>, JpaSpecificationExecutor<Cupon> {
    Optional<Cupon> findByCodigoIgnoreCase(String codigo);
}
