package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.Submodulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmoduloRepository extends JpaRepository<Submodulo, Long> {
    List<Submodulo> findAllByActivoTrueOrderByModulo_OrdenAscOrdenAsc();
}
