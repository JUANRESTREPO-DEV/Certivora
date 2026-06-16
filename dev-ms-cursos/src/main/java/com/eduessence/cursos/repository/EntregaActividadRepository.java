package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.EntregaActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntregaActividadRepository extends JpaRepository<EntregaActividad, Long> {
    List<EntregaActividad> findAllByMatriculaIdOrderByFechaEntregaDesc(Long matriculaId);
    List<EntregaActividad> findAllByActividadIdOrderByFechaEntregaDesc(Long actividadId);
    Optional<EntregaActividad> findTopByMatriculaIdAndActividadIdOrderByNumeroEntregaDesc(Long matriculaId, Long actividadId);
}
