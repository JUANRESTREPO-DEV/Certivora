package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.ProgresoLeccion;
import com.eduessence.cursos.model.enums.EstadoLeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgresoLeccionRepository extends JpaRepository<ProgresoLeccion, Long> {
    Optional<ProgresoLeccion> findByMatriculaIdAndLeccionId(Long matriculaId, Long leccionId);
    List<ProgresoLeccion> findAllByMatriculaId(Long matriculaId);
    long countByMatriculaIdAndEstado(Long matriculaId, EstadoLeccion estado);
}
