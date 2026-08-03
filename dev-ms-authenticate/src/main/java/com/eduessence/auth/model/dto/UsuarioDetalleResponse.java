package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Ficha completa del usuario para el drawer detalle. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDetalleResponse {
    private Long id;
    private String email;
    private String username;
    private String avatarUrl;
    private String estado;
    private Boolean emailVerificado;

    private String nombres;
    private String apellidos;
    private String tipoDocumento;
    private String documento;
    private String telefono;
    private String pais;
    private String ciudad;
    private String profesion;
    private String institucion;

    private LocalDateTime ultimoAcceso;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime bloqueadoHasta;

    private List<RolDto> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolDto {
        private Long id;
        private String codigo;
        private String nombre;
    }
}
