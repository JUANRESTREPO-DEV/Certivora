package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.Usuario;
import com.eduessence.auth.model.enums.EstadoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    /** Página de usuarios en un estado determinado. Para broadcasts internos. */
    Page<Usuario> findAllByEstado(EstadoUsuario estado, Pageable pageable);
}
