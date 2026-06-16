package com.eduessence.streaming.repository;

import com.eduessence.streaming.model.entity.StreamSession;
import com.eduessence.streaming.model.enums.EstadoStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    Optional<StreamSession> findByStreamKey(String streamKey);
    List<StreamSession> findAllByEstado(EstadoStream estado);
}
