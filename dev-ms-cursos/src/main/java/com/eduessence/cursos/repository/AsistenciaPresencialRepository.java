package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.AsistenciaPresencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaPresencialRepository extends JpaRepository<AsistenciaPresencial, Long> {

    Optional<AsistenciaPresencial> findByMatriculaIdAndSesionVirtualId(Long matriculaId,
                                                                       Long sesionVirtualId);

    List<AsistenciaPresencial> findAllBySesionVirtualId(Long sesionVirtualId);

    long countBySesionVirtualId(Long sesionVirtualId);
}
