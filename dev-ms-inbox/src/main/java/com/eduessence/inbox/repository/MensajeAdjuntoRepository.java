package com.eduessence.inbox.repository;

import com.eduessence.inbox.model.entity.MensajeAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeAdjuntoRepository extends JpaRepository<MensajeAdjunto, Long> {
    List<MensajeAdjunto> findAllByMensajeId(Long mensajeId);
}
