package com.eduessence.sendmail.repository;

import com.eduessence.sendmail.model.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    Optional<EmailTemplate> findByNombreIgnoreCase(String nombre);

    List<EmailTemplate> findAllByActivoTrueOrderByNombreAsc();

    List<EmailTemplate> findAllByOrderByNombreAsc();

    @Query("SELECT t FROM EmailTemplate t LEFT JOIN FETCH t.variables WHERE LOWER(t.nombre) = LOWER(:nombre) AND t.activo = true")
    Optional<EmailTemplate> findActivoConVariables(String nombre);
}
