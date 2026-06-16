package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.PreguntaExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreguntaExamenRepository extends JpaRepository<PreguntaExamen, Long> {
    List<PreguntaExamen> findAllByExamen_IdOrderByOrdenAsc(Long examenId);
}
