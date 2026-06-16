package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.AsistenciaVirtual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaVirtualRepository extends JpaRepository<AsistenciaVirtual, Long> {
    Optional<AsistenciaVirtual> findByMatriculaIdAndSesionVirtualId(Long matriculaId, Long sesionVirtualId);
    List<AsistenciaVirtual> findAllByMatriculaId(Long matriculaId);
}
