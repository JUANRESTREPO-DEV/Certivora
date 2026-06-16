package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.CursoRecurso;
import com.eduessence.cursos.model.enums.TipoRecurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRecursoRepository extends JpaRepository<CursoRecurso, Long> {

    List<CursoRecurso> findByCursoIdOrderByOrdenAsc(Long cursoId);

    List<CursoRecurso> findByCursoIdAndTipoOrderByOrdenAsc(Long cursoId, TipoRecurso tipo);

    long countByCursoIdAndTipo(Long cursoId, TipoRecurso tipo);

    void deleteByCursoId(Long cursoId);
}
