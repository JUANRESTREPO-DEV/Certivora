package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.SesionVirtual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionVirtualRepository extends
        JpaRepository<SesionVirtual, Long>,
        JpaSpecificationExecutor<SesionVirtual> {
    List<SesionVirtual> findAllByCursoIdOrderByFechaInicioAsc(Long cursoId);
    Optional<SesionVirtual> findByStreamSessionId(String streamSessionId);
}
