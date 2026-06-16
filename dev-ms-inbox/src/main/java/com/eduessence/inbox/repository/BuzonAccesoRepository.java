package com.eduessence.inbox.repository;

import com.eduessence.inbox.model.entity.BuzonAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuzonAccesoRepository extends JpaRepository<BuzonAcceso, Long> {

    Optional<BuzonAcceso> findByBuzonIdAndUsuarioId(Long buzonId, Long usuarioId);

    List<BuzonAcceso> findAllByUsuarioId(Long usuarioId);

    List<BuzonAcceso> findAllByBuzonId(Long buzonId);

    boolean existsByBuzonIdAndUsuarioId(Long buzonId, Long usuarioId);

    void deleteByBuzonIdAndUsuarioId(Long buzonId, Long usuarioId);
}
