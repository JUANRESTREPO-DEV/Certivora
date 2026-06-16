package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.AccionPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccionPermisoRepository extends JpaRepository<AccionPermiso, Long> {
}
