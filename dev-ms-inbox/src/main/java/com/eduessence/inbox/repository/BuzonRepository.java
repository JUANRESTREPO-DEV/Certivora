package com.eduessence.inbox.repository;

import com.eduessence.inbox.model.entity.Buzon;
import com.eduessence.inbox.model.enums.TipoBuzon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuzonRepository extends JpaRepository<Buzon, Long> {

    Optional<Buzon> findByDireccionIgnoreCase(String direccion);

    boolean existsByDireccionIgnoreCase(String direccion);

    Optional<Buzon> findFirstByTipoAndActivoTrue(TipoBuzon tipo);

    Page<Buzon> findAllByActivo(Boolean activo, Pageable pageable);

    List<Buzon> findAllByTipoAndActivoTrue(TipoBuzon tipo);
}
