package com.eduessence.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmoduloDef {
    private Long id;
    private String codigo;
    private String label;
    /** Ruta Angular exacta (debe coincidir con app.routes.ts). */
    private String path;
    private String icon;
    private Integer orden;
    /** Permiso mínimo para acceder (puede ser null = público dentro del back-office). */
    private String permisoEntrada;
}
