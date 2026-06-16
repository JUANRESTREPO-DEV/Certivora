package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.UsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, Long> {

    List<UsuarioPermiso> findAllByUsuario_Id(Long usuarioId);

    Optional<UsuarioPermiso> findByUsuario_IdAndPermiso_Codigo(Long usuarioId, String codigoPermiso);

    void deleteByUsuario_Id(Long usuarioId);
}
