package com.eduessence.sendmail.repository;

import com.eduessence.sendmail.model.entity.EmailEnvio;
import com.eduessence.sendmail.model.enums.EstadoEnvio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EmailEnvioRepository extends JpaRepository<EmailEnvio, Long> {

    Page<EmailEnvio> findAllByDestinatarioContainingIgnoreCaseOrderByFechaEnvioDesc(
            String destinatario, Pageable pageable
    );

    Page<EmailEnvio> findAllByOrderByFechaEnvioDesc(Pageable pageable);

    /**
     * Búsqueda admin con filtros opcionales. Cualquier parámetro {@code null}
     * se ignora — así el front puede combinar libremente.
     *
     * @param estado     ENVIADO / FALLIDO / REINTENTAR (opcional)
     * @param templateId ID del template (opcional)
     * @param desde      lower bound inclusivo de fecha_envio (opcional)
     * @param hasta      upper bound inclusivo (opcional)
     * @param q          substring case-insensitive sobre destinatario o asunto (opcional)
     */
    @Query("SELECT e FROM EmailEnvio e " +
            "WHERE (:estado IS NULL OR e.estado = :estado) " +
            "  AND (:templateId IS NULL OR e.template.id = :templateId) " +
            "  AND (:desde IS NULL OR e.fechaEnvio >= :desde) " +
            "  AND (:hasta IS NULL OR e.fechaEnvio <= :hasta) " +
            "  AND (:q IS NULL OR :q = '' " +
            "       OR LOWER(e.destinatario) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "       OR LOWER(e.asunto)       LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY e.fechaEnvio DESC")
    Page<EmailEnvio> buscar(@Param("estado") EstadoEnvio estado,
                            @Param("templateId") Long templateId,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta,
                            @Param("q") String q,
                            Pageable pageable);

    long countByEstado(EstadoEnvio estado);
}
