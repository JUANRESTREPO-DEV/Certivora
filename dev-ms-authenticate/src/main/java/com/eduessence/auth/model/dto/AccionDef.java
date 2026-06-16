package com.eduessence.auth.model.dto;

import com.eduessence.auth.model.enums.AccionEstado;
import com.eduessence.auth.model.enums.AccionTipo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Bundle de permisos con su estado calculado para un usuario. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccionDef {
    private Long id;
    private String codigo;
    private String label;
    private String descripcion;
    private String icon;
    private AccionTipo tipo;
    private Integer orden;
    private Long submoduloId;
    private String submoduloCodigo;
    private AccionEstado estado;
    private List<String> permisosRequeridos;
    private List<String> permisosOpcionales;
    private List<String> permisosConcedidos;
}
