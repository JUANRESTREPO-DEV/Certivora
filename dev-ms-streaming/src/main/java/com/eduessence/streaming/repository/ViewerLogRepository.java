package com.eduessence.streaming.repository;

import com.eduessence.streaming.model.entity.ViewerLog;
import com.eduessence.streaming.model.enums.EventoViewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewerLogRepository extends JpaRepository<ViewerLog, Long> {
    List<ViewerLog> findAllByStreamSessionIdAndUsuarioIdOrderByTimestampEventoAsc(Long streamSessionId, Long usuarioId);
    long countByStreamSessionIdAndEvento(Long streamSessionId, EventoViewer evento);
}
