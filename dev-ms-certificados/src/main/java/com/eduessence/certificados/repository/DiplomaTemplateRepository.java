package com.eduessence.certificados.repository;

import com.eduessence.certificados.model.entity.DiplomaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;

@Repository
public interface DiplomaTemplateRepository extends JpaRepository<DiplomaTemplate, Long> {

    Optional<DiplomaTemplate> findByCursoIdAndTipoParticipanteAndActivoTrue(Long cursoId, String tipoParticipante);

    /** Plantilla global (sin curso_id) por tipo de participante. */
    Optional<DiplomaTemplate> findByCursoIdIsNullAndTipoParticipanteAndActivoTrue(String tipoParticipante);

    /** Plantillas reutilizables (sin curso) + las del curso indicado. */
    List<DiplomaTemplate> findByActivoTrueAndCursoIdIsNullOrCursoId(Long cursoId);

    List<DiplomaTemplate> findByActivoTrueOrderByNombreAsc();

    List<DiplomaTemplate> findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String q);
}
