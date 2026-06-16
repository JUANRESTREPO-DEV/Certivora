package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.Accion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccionRepository extends JpaRepository<Accion, Long> {

    /**
     * Trae todas las acciones activas con su submódulo + módulo + permisos
     * en una sola query para evitar N+1 al armar los DTOs.
     */
    @Query("""
            SELECT DISTINCT a FROM Accion a
              JOIN FETCH a.submodulo s
              JOIN FETCH s.modulo m
              LEFT JOIN FETCH a.permisos ap
              LEFT JOIN FETCH ap.permiso
            WHERE a.activo = true AND s.activo = true AND m.activo = true
            ORDER BY m.orden, s.orden, a.orden
            """)
    List<Accion> findAllActivasConPermisos();
}
