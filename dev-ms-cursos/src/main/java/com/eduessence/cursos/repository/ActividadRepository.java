package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    List<Actividad> findAllByCursoId(Long cursoId);
}
