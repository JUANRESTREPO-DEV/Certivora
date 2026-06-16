package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.OpcionPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpcionPreguntaRepository extends JpaRepository<OpcionPregunta, Long> {
    List<OpcionPregunta> findAllByPregunta_IdOrderByOrdenAsc(Long preguntaId);
}
