package com.eduessence.inbox.repository;

import com.eduessence.inbox.model.entity.Mensaje;
import com.eduessence.inbox.model.enums.Carpeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    Optional<Mensaje> findByMessageId(String messageId);

    Page<Mensaje> findAllByBuzonIdAndCarpetaOrderByFechaRecibidoDesc(Long buzonId, Carpeta carpeta, Pageable pageable);

    long countByBuzonIdAndCarpetaAndLeidoFalse(Long buzonId, Carpeta carpeta);

    List<Mensaje> findAllByThreadIdOrderByFechaRecibidoAsc(String threadId);

    @Query("SELECT m FROM Mensaje m WHERE m.buzon.id = :buzonId AND " +
           "(LOWER(m.asunto) LIKE LOWER(CONCAT('%', :q, '%')) " +
           " OR LOWER(m.remitenteEmail) LIKE LOWER(CONCAT('%', :q, '%')) " +
           " OR LOWER(m.snippet) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Mensaje> buscar(@Param("buzonId") Long buzonId, @Param("q") String q, Pageable pageable);
}
