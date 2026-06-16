package com.eduessence.inbox.repository;

import com.eduessence.inbox.model.entity.BuzonAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuzonAliasRepository extends JpaRepository<BuzonAlias, Long> {

    Optional<BuzonAlias> findByDireccionIgnoreCase(String direccion);

    boolean existsByDireccionIgnoreCase(String direccion);

    List<BuzonAlias> findAllByBuzonId(Long buzonId);
}
