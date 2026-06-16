package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.ConfiguracionAprobacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionAprobacionRepository extends JpaRepository<ConfiguracionAprobacion, Long> {
    Optional<ConfiguracionAprobacion> findByCursoId(Long cursoId);
}
