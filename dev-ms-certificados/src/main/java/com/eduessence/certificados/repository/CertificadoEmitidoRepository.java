package com.eduessence.certificados.repository;

import com.eduessence.certificados.model.entity.CertificadoEmitido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificadoEmitidoRepository extends JpaRepository<CertificadoEmitido, Long> {
    Optional<CertificadoEmitido> findByCodigo(String codigo);
    Optional<CertificadoEmitido> findByMatriculaId(Long matriculaId);
    List<CertificadoEmitido> findAllByUsuarioIdOrderByFechaEmisionDesc(Long usuarioId);
    long countByFechaEmisionBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    long countByCursoId(Long cursoId);

    /** Devuelve [cursoId, count] para poblar el reporte de certificados. */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT curso_id AS cursoId, COUNT(*) AS emitidos
            FROM certificado_emitido
            GROUP BY curso_id
            """, nativeQuery = true)
    List<Object[]> countEmitidosPorCurso();
}
