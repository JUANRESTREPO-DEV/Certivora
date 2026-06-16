package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    @Query("SELECT l FROM Leccion l WHERE l.seccion.curso.id = :cursoId ORDER BY l.seccion.orden, l.orden")
    List<Leccion> findAllByCursoId(@Param("cursoId") Long cursoId);

    @Query("SELECT COUNT(l) FROM Leccion l WHERE l.seccion.curso.id = :cursoId")
    long countByCursoId(@Param("cursoId") Long cursoId);

    /**
     * Lecciones vinculadas a un examen específico. Cuando un alumno finaliza
     * un intento, estas lecciones se marcan como COMPLETADA para que cuenten
     * en el porcentaje de progreso del curso.
     */
    List<Leccion> findAllByExamenId(Long examenId);
}
