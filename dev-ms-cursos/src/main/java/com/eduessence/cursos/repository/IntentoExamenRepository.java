package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.IntentoExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntentoExamenRepository extends JpaRepository<IntentoExamen, Long> {
    List<IntentoExamen> findAllByMatriculaIdAndExamenIdOrderByNumeroIntentoDesc(Long matriculaId, Long examenId);
    long countByMatriculaIdAndExamenId(Long matriculaId, Long examenId);
}
