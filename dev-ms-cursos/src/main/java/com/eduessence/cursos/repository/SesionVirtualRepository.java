package com.eduessence.cursos.repository;

import com.eduessence.cursos.model.entity.SesionVirtual;
import com.eduessence.cursos.model.enums.TipoSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionVirtualRepository extends
        JpaRepository<SesionVirtual, Long>,
        JpaSpecificationExecutor<SesionVirtual> {
    List<SesionVirtual> findAllByCursoIdOrderByFechaInicioAsc(Long cursoId);
    Optional<SesionVirtual> findByStreamSessionId(String streamSessionId);

    /** Para check-in: busca la sesión por el QR escaneado. */
    Optional<SesionVirtual> findByQrToken(String qrToken);

    /** Sesiones presenciales en un rango — usado por el panel de check-in. */
    List<SesionVirtual> findAllByTipoAndFechaInicioBetweenOrderByFechaInicioAsc(
            TipoSesion tipo, LocalDateTime desde, LocalDateTime hasta);
}
