package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    List<Seccion> findAllByCurso_IdOrderByOrdenAsc(Long cursoId);
}
