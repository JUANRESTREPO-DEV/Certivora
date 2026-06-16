package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {
    List<Modulo> findAllByActivoTrueOrderByOrdenAsc();
}
