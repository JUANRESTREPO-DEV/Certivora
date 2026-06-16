package com.eduessence.auth.service.impl;

import com.eduessence.auth.exception.AuthApiException;
import com.eduessence.auth.exception.ServerApiStatusCode;
import com.eduessence.auth.model.dto.ActualizarPerfilRequest;
import com.eduessence.auth.model.dto.PerfilResponse;
import com.eduessence.auth.model.entity.Persona;
import com.eduessence.auth.model.entity.Usuario;
import com.eduessence.auth.repository.UsuarioRepository;
import com.eduessence.auth.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfilServiceImpl implements PerfilService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse obtener(Long usuarioId) {
        return toResponse(findUsuario(usuarioId));
    }

    @Override
    @Transactional
    public PerfilResponse actualizar(Long usuarioId, ActualizarPerfilRequest req) {
        Usuario usuario = findUsuario(usuarioId);
        Persona persona = usuario.getPersona();
        if (persona == null) {
            persona = Persona.builder().usuario(usuario).build();
            usuario.setPersona(persona);
        }
        if (req.getNombres() != null) persona.setNombres(req.getNombres().trim());
        if (req.getApellidos() != null) persona.setApellidos(req.getApellidos().trim());
        if (req.getDocumento() != null) persona.setDocumento(req.getDocumento().trim());
        if (req.getTipoDocumento() != null) persona.setTipoDocumento(req.getTipoDocumento().trim());
        if (req.getTelefono() != null) persona.setTelefono(req.getTelefono().trim());
        if (req.getPais() != null) persona.setPais(req.getPais().trim());
        if (req.getCiudad() != null) persona.setCiudad(req.getCiudad().trim());
        if (req.getProfesion() != null) persona.setProfesion(req.getProfesion().trim());
        if (req.getInstitucion() != null) persona.setInstitucion(req.getInstitucion().trim());

        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    @Override
    @Transactional
    public PerfilResponse actualizarAvatar(Long usuarioId, String avatarUrl) {
        Usuario usuario = findUsuario(usuarioId);
        usuario.setAvatarUrl(avatarUrl);
        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    private Usuario findUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AuthApiException(ServerApiStatusCode.USUARIO_NO_ENCONTRADO));
    }

    private PerfilResponse toResponse(Usuario u) {
        Persona p = u.getPersona();
        return PerfilResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .nombres(p != null ? p.getNombres() : null)
                .apellidos(p != null ? p.getApellidos() : null)
                .documento(p != null ? p.getDocumento() : null)
                .tipoDocumento(p != null ? p.getTipoDocumento() : null)
                .telefono(p != null ? p.getTelefono() : null)
                .pais(p != null ? p.getPais() : null)
                .ciudad(p != null ? p.getCiudad() : null)
                .profesion(p != null ? p.getProfesion() : null)
                .institucion(p != null ? p.getInstitucion() : null)
                .avatarUrl(u.getAvatarUrl())
                .emailVerificado(u.getEmailVerificado())
                .roles(u.getRoles().stream().map(r -> r.getCodigo()).collect(Collectors.toSet()))
                .build();
    }
}
