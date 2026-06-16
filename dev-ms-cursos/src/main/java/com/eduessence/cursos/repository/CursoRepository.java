package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.Curso;
import com.eduessence.cursos.model.enums.EstadoCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    Optional<Curso> findBySlug(String slug);
    List<Curso> findAllByEstadoOrderByFechaInicioDesc(EstadoCurso estado);
    List<Curso> findAllByOrderByFechaCreacionDesc();
}
