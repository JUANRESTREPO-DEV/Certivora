package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.EscarapelaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscarapelaTemplateRepository extends JpaRepository<EscarapelaTemplate, Long> {
    Optional<EscarapelaTemplate> findByCursoId(Long cursoId);
}
